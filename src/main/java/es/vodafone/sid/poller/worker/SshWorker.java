// worker/SshWorker.java
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class SshWorker implements Callable<List<MetricRecord>> {
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
      String username = protocol.config().get("username").asString();
      String password = protocol.config().get("password").asString();
      int port = protocol.config().get("port").asInt(22);
      long timeout = protocol.config().get("connectTimeout").asLong(10000);

      try (ClientSession session = sshClient
          .connect(username, host, port)
          .verify(timeout, TimeUnit.MILLISECONDS)
          .getSession()) {

        session.addPasswordIdentity(password);
        session.auth().verify(timeout, TimeUnit.MILLISECONDS);

        // Agrupar por address para el tipo 7
        Map<String, List<SourceRecord>> byAddress = sources.stream()
            .collect(Collectors.groupingBy(SourceRecord::address));

        List<MetricRecord> metrics = new ArrayList<>();

        for (Map.Entry<String, List<SourceRecord>> entry : byAddress.entrySet()) {
          String command = entry.getKey();
          List<SourceRecord> group = entry.getValue();
          try {
            String rawValue = executeCommand(session, command, timeout);
            if (rawValue != null) {
              short type = group.getFirst().type();
              metrics.addAll(sourceTypeRegistry.get(type).apply(rawValue, group, instant));
            }
          } catch (RuntimeException e) {
            log.warn("Could not build SSH metrics for command '{}' on {}", command, host, e);
          }
        }

        return MetricRecords.complete(sources, metrics, instant);
      }

    } catch (IOException | RuntimeException e) {
      log.error("SSH connection failed to {}", host, e);
      return MetricRecords.nullValues(sources, instant);
    }
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
