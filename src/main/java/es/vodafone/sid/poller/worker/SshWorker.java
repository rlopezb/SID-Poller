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
  public List<Source> getSources() {
    return sources;
  }

  @Override
  public List<Metric> call() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    Map<Short, Metric> metricsMap = new HashMap<>();

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
        // Single sources
        for (Source source : sources) {
          if (source.isMulti()) {
            continue;
          }
          Metric metric = BaseSourceType.nullMetric(source, now);
          String rawValue = executeCommand(session, source.address());
          try {
            List<Metric> metrics = sourceTypeRegistry.get(source.type()).calculate(rawValue, List.of(source), now);
            if (metrics != null && !metrics.isEmpty() && metrics.getFirst() != null && metrics.size() == 1) {
              metric = metrics.getFirst();
            } else {
              log.warn("Sigle source {} returned wrong metric", source.name());
            }
          } catch (RuntimeException e) {
            log.warn("Could not measure source {}", source.name(), e);
          }
          metricsMap.put(source.id(), metric);
        }

        // Multi sources
        Map<String, List<Source>> multiSources = new HashMap<>();
        for (Source source : sources) {
          if (source.isMulti() && source.address() != null) {
            multiSources.computeIfAbsent(source.address(), _ -> new ArrayList<>()).add(source);
          }
        }
        for (Map.Entry<String, List<Source>> multiSource : multiSources.entrySet()) {
          String address = multiSource.getKey();
          List<Source> groupedSources = multiSource.getValue();
          try {
            String rawValue = executeCommand(session, address);
            List<Metric> multiMetrics = rawValue == null
                ? List.of()
                : sourceTypeRegistry.get(SourceTypeRegistry.getMulti()).calculate(rawValue, groupedSources, now);
            if (multiMetrics != null) {
              multiMetrics.forEach(metric -> metricsMap.put(metric.srcId(), metric));
            }
          } catch (RuntimeException e) {
            log.warn("Could not measure multi-source command '{}' on {}", multiSource.getKey(), element.name(), e);
          }
        }
      }
    } catch (IOException | RuntimeException e) {
      log.error("SSH collection failed to {}", element.name(), e);
    }
    return buildMetrics(sources, metricsMap, now);
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