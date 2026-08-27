package es.vodafone.sid.poller.worker;

import es.vodafone.sid.poller.model.ElementRecord;
import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.model.ProtocolRecord;
import es.vodafone.sid.poller.model.SourceRecord;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class SshWorker implements Worker {
  private final ElementRecord elementRecord;
  private final List<SourceRecord> sourceRecords;
  private final ProtocolRecord protocolRecord;
  private final SshClient sshClient;
  private final SourceTypeRegistry sourceTypeRegistry;

  @Override
  public List<MetricRecord> call() {
    List<SourceRecord> sources = sourceRecords == null ? List.of() : sourceRecords;
    OffsetDateTime instant = OffsetDateTime.now(ZoneOffset.UTC);

    try {
      String host = elementRecord.name();
      var config = protocolRecord.config();
      String username = config.get("username").asString();
      String password = config.get("password").asString();
      int port = config.get("port").asInt(22);
      long timeout = config.get("connectTimeout").asLong(10000);

      try (ClientSession session = sshClient.connect(username, host, port)
          .verify(timeout, TimeUnit.MILLISECONDS)
          .getSession()) {
        session.addPasswordIdentity(password);
        session.auth().verify(timeout, TimeUnit.MILLISECONDS);

        Map<Short, MetricRecord> metricsBySourceId = new HashMap<>();
        sources.stream()
            .filter(source -> !source.isMulti())
            .forEach(source -> metricsBySourceId.put(
                source.id(), measure(source, executeCommand(session, source.address(), timeout), instant)));

        Map<String, List<SourceRecord>> multiSources = sources.stream()
            .filter(SourceRecord::isMulti)
            .filter(source -> source.address() != null)
            .collect(Collectors.groupingBy(SourceRecord::address));
        for (Map.Entry<String, List<SourceRecord>> entry : multiSources.entrySet()) {
          try {
            String rawValue = executeCommand(session, entry.getKey(), timeout);
            List<MetricRecord> metrics = rawValue == null
                ? List.of()
                : sourceTypeRegistry.get(SourceTypeRegistry.getMulti())
                    .apply(rawValue, entry.getValue(), instant);
            if (metrics != null) {
              metrics.forEach(metric -> metricsBySourceId.put(metric.srcId(), metric));
            }
          } catch (RuntimeException e) {
            log.warn("Could not measure multi-source command '{}' on {}", entry.getKey(), host, e);
          }
        }

        return sources.stream()
            .map(source -> metricsBySourceId.getOrDefault(
                source.id(), BaseSourceType.nullMetric(source, instant)))
            .toList();
      }
    } catch (IOException | RuntimeException e) {
      log.error("SSH collection failed to {}", elementRecord.name(), e);
      return nullMetrics(sources, instant);
    }
  }

  private MetricRecord measure(SourceRecord sourceRecord, String rawValue, OffsetDateTime instant) {
    if (rawValue == null || rawValue.isBlank()) {
      return BaseSourceType.nullMetric(sourceRecord, instant);
    }

    try {
      List<MetricRecord> metrics = sourceTypeRegistry.get(sourceRecord.type())
          .apply(rawValue, List.of(sourceRecord), instant);
      return metrics != null && metrics.size() == 1 && metrics.getFirst() != null
          ? metrics.getFirst()
          : BaseSourceType.nullMetric(sourceRecord, instant);
    } catch (RuntimeException e) {
      log.warn("Could not measure source {}", sourceRecord.name(), e);
      return BaseSourceType.nullMetric(sourceRecord, instant);
    }
  }

  private List<MetricRecord> nullMetrics(List<SourceRecord> sources, OffsetDateTime instant) {
    return sources.stream()
        .map(source -> BaseSourceType.nullMetric(source, instant))
        .toList();
  }

  private String executeCommand(ClientSession session, String command, long timeout) {
    log.debug("Executing ssh command: {}", command);
    try (ChannelExec channel = session.createExecChannel(command)) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      channel.setOut(output);
      channel.open().verify(timeout, TimeUnit.MILLISECONDS);
      var events = channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), timeout);
      if (!events.contains(ClientChannelEvent.CLOSED)) {
        channel.close(true);
        log.warn("SSH command '{}' timed out on {}", command, elementRecord.name());
        return null;
      }
      Integer exitStatus = channel.getExitStatus();
      if (exitStatus != null && exitStatus != 0) {
        log.warn("SSH command '{}' exited with status {} on {}", command, exitStatus, elementRecord.name());
        return null;
      }
      String result = output.toString(StandardCharsets.UTF_8).trim();
      log.debug("SSH command result: {}", result);
      return result.isBlank() ? null : result;
    } catch (IOException | RuntimeException e) {
      log.error("Command '{}' failed on {}", command, elementRecord.name(), e);
      return null;
    }
  }
}
