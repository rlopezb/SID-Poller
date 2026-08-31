package es.vodafone.sid.poller.worker;

import es.vodafone.sid.poller.model.Element;
import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Protocol;
import es.vodafone.sid.poller.model.Source;
import es.vodafone.sid.poller.strategy.BaseSourceType;
import es.vodafone.sid.poller.strategy.SourceTypeRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class SshWorker implements Worker {
  private final Element element;
  private final List<Source> sources;
  private final Protocol protocol;
  private final SshClient sshClient;
  private final SourceTypeRegistry sourceTypeRegistry;

  @Override
  public List<Metric> call() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    try {
      String host = element.name();
      String username = protocol.config().get("username").asString();
      String password = protocol.config().get("password").asString();
      int port = protocol.config().get("port").asInt(22);
      long connectTimeout = protocol.config().get("connectTimeout").asLong(10000);

      try (ClientSession session = sshClient.connect(username, host, port)
          .verify(connectTimeout, TimeUnit.MILLISECONDS)
          .getSession()) {
        session.addPasswordIdentity(password);
        session.auth().verify(connectTimeout, TimeUnit.MILLISECONDS);

        // Metrics map by source
        Map<Short, Metric> metricMap = new HashMap<>();
        // Single source metricMap
        for (Source source : sources) {
          if (!source.isMulti()) {
            String rawValue = executeCommand(session, source.address());
            metricMap.put(source.id(), measure(source, rawValue, now));
          }
        }

        // Multi source metricMap
        Map<String, List<Source>> multiSources = new HashMap<>();
        for (Source source : sources) {
          if (source.isMulti()) {
            if (source.address() != null) {
              multiSources.computeIfAbsent(source.address(), k -> new ArrayList<>()).add(source);
            }
          }
        }
        for (Map.Entry<String, List<Source>> entry : multiSources.entrySet()) {
          String address =  entry.getKey();
          List<Source> sources = entry.getValue();
          try {
            String rawValue = executeCommand(session, address);
            List<Metric> multiMetrics = rawValue == null
                ? List.of()
                : sourceTypeRegistry.get(SourceTypeRegistry.getMulti())
                    .apply(rawValue, sources, now);
            if (multiMetrics != null) {
              multiMetrics.forEach(metric -> metricMap.put(metric.srcId(), metric));
            }
          } catch (RuntimeException e) {
            log.warn("Could not measure multi-source command '{}' on {}", entry.getKey(), host, e);
          }
        }

        List<Metric> metrics = new ArrayList<>();
        for (Source source : sources) {
          Metric orDefault = metricMap.getOrDefault(
              source.id(), BaseSourceType.nullMetric(source, now));
          metrics.add(orDefault);
        }
        return metrics;
      }
    } catch (IOException | RuntimeException e) {
      log.error("SSH collection failed to {}", element.name(), e);
      return nullMetrics(sources, now);
    }
  }

  private Metric measure(Source source, String rawValue, OffsetDateTime instant) {
    if (rawValue == null || rawValue.isBlank()) {
      return BaseSourceType.nullMetric(source, instant);
    }

    try {
      List<Metric> metrics = sourceTypeRegistry.get(source.type())
          .apply(rawValue, List.of(source), instant);
      return metrics != null && metrics.size() == 1 && metrics.getFirst() != null
          ? metrics.getFirst()
          : BaseSourceType.nullMetric(source, instant);
    } catch (RuntimeException e) {
      log.warn("Could not measure source {}", source.name(), e);
      return BaseSourceType.nullMetric(source, instant);
    }
  }

  private List<Metric> nullMetrics(List<Source> sources, OffsetDateTime instant) {
    List<Metric> metrics = new ArrayList<>();
    for (Source source : sources) {
      Metric metric = BaseSourceType.nullMetric(source, instant);
      metrics.add(metric);
    }
    return metrics;
  }

  private String executeCommand(ClientSession session, String command) {
    log.debug("Executing ssh command: {}", command);
    long execTimeout = protocol.config().get("execTimeout").asLong(10000);
    try (ChannelExec channel = session.createExecChannel(command)) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      channel.setOut(output);
      channel.open().verify(execTimeout, TimeUnit.MILLISECONDS);
      var events = channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), execTimeout);
      if (!events.contains(ClientChannelEvent.CLOSED)) {
        channel.close(true);
        log.warn("SSH command '{}' timed out on {}", command, element.name());
        return null;
      }
      Integer exitStatus = channel.getExitStatus();
      if (exitStatus != null && exitStatus != 0) {
        log.warn("SSH command '{}' exited with status {} on {}", command, exitStatus, element.name());
        return null;
      }
      String result = output.toString(StandardCharsets.UTF_8).trim();
      log.debug("SSH command result: {}", result);
      return result.isBlank() ? null : result;
    } catch (IOException | RuntimeException e) {
      log.error("Command '{}' failed on {}", command, element.name(), e);
      return null;
    }
  }
}
