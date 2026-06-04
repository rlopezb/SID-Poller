package es.vodafone.sid.poller.worker;

import es.vodafone.sid.poller.model.ElementRecord;
import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.model.MetricRecords;
import es.vodafone.sid.poller.model.ProtocolRecord;
import es.vodafone.sid.poller.model.SourceRecord;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class SshWorker implements Callable<List<MetricRecord>> {
  private static final short TYPE_MULTI_CAPTURE = 7;

  private final ElementRecord element;
  private final List<SourceRecord> sources;
  private final ProtocolRecord protocol;
  private final SshClient sshClient;
  private final SourceTypeRegistry sourceTypeRegistry;

  @Override
  public List<MetricRecord> call() {
    OffsetDateTime instant = OffsetDateTime.now(ZoneOffset.UTC);
    String host = element.name();

    try {
      long timeout = protocol.config().get("connectTimeout").asLong(10000);

      try (ClientSession session = connect(timeout)) {
        authenticate(session, timeout);
        return MetricRecords.complete(sources, collectMetrics(session, timeout, instant), instant);
      }

    } catch (IOException | RuntimeException e) {
      log.error("SSH connection failed to {}", host, e);
      return MetricRecords.nullValues(sources, instant);
    }
  }

  private ClientSession connect(long timeout) throws IOException {
    return sshClient.connect(
            protocol.config().get("username").asString(),
            element.name(),
            protocol.config().get("port").asInt(22)
        )
        .verify(timeout, TimeUnit.MILLISECONDS)
        .getSession();
  }

  private void authenticate(ClientSession session, long timeout) throws IOException {
    session.addPasswordIdentity(protocol.config().get("password").asString());
    session.auth().verify(timeout, TimeUnit.MILLISECONDS);
  }

  private List<MetricRecord> collectMetrics(ClientSession session, long timeout, OffsetDateTime instant) {
    List<MetricRecord> metrics = new ArrayList<>();
    for (List<SourceRecord> group : commandGroups()) {
      metrics.addAll(collectMetrics(session, group, timeout, instant));
    }
    return metrics;
  }

  private List<MetricRecord> collectMetrics(
      ClientSession session,
      List<SourceRecord> group,
      long timeout,
      OffsetDateTime instant
  ) {
    String command = group.getFirst().address();
    try {
      String rawValue = executeCommand(session, command, timeout);
      if (rawValue == null) {
        return List.of();
      }
      short type = group.getFirst().type();
      return sourceTypeRegistry.get(type).apply(rawValue, group, instant);
    } catch (RuntimeException e) {
      log.warn("Could not build SSH metrics for command '{}' on {}", command, element.name(), e);
      return List.of();
    }
  }

  private List<List<SourceRecord>> commandGroups() {
    List<List<SourceRecord>> groups = new ArrayList<>();
    Map<String, List<SourceRecord>> multiCaptureByAddress = new LinkedHashMap<>();

    for (SourceRecord source : sources) {
      if (source.type() == TYPE_MULTI_CAPTURE) {
        multiCaptureByAddress.computeIfAbsent(source.address(), address -> new ArrayList<>()).add(source);
      } else {
        groups.add(List.of(source));
      }
    }

    groups.addAll(multiCaptureByAddress.values());
    return groups;
  }

  private String executeCommand(ClientSession session, String command, long timeout) {
    log.debug("Executing ssh command: {}", command);
    try (ChannelExec channel = session.createExecChannel(command)) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      channel.setOut(output);
      channel.open().verify(timeout, TimeUnit.MILLISECONDS);
      channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), timeout);
      String result = output.toString(StandardCharsets.UTF_8).trim();
      log.debug("SSH command result: {}", result);
      return result;
    } catch (IOException | RuntimeException e) {
      log.error("Command '{}' failed on {}", command, element.name(), e);
      return null;
    }
  }
}
