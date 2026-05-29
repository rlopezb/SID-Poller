// worker/SshWorker.java
package es.vodafone.sid.poller.worker;

import es.vodafone.sid.poller.model.ElementRecord;
import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.model.ProtocolRecord;
import es.vodafone.sid.poller.model.SourceRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class SshWorker implements Callable<List<MetricRecord>> {
  private final ElementRecord element;
  private final List<SourceRecord> sources;
  private final ProtocolRecord protocol;
  private final SshClient sshClient;

  @Override
  public List<MetricRecord> call() {
    String host = element.name();
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

      return sources.stream()
          .map(source -> executeCommand(session, source))
          .toList();

    } catch (IOException e) {
      log.error("SSH connection failed to {}", host, e);
      return List.of();
    }
  }

  private MetricRecord executeCommand(ClientSession session, SourceRecord source) {
    log.debug("Executing ssh command: {}", source.address());
    try (ChannelExec channel = session.createExecChannel(source.address())) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      channel.setOut(output);
      channel.open().verify(protocol.config().get("timeout").asInt(), TimeUnit.MILLISECONDS);
      channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), protocol.config().get("timeout").asInt());

      String result = output.toString(StandardCharsets.UTF_8).trim();
      log.debug("SSH command result {}", result);
      BigInteger value = parseValue(result, source);

      return new MetricRecord(
          OffsetDateTime.now(ZoneOffset.UTC),
          source.id(),
          source.elementId(),
          source.elementTypeId(),
          source.siteId(),
          source.cdcId(),
          source.zoneId(),
          source.netId(),
          source.archId(),
          source.groupId(),
          source.serviceId(),
          source.serviceTypeId(),
          value
      );
    } catch (IOException e) {
      log.error("Command '{}' failed on {}", source.address(), source.name(), e);
      return null;
    }
  }

  private BigInteger parseValue(String result, SourceRecord source) {
    try {
      if (source.capture() != null && !source.capture().isBlank()) {
        Pattern pattern = Pattern.compile(source.capture());
        Matcher matcher = pattern.matcher(result);
        if (matcher.find()) {
          return new BigInteger(matcher.group(1));
        }
        log.warn("Capture pattern '{}' did not match output for source {}",
            source.capture(), source.name());
      } else {
        return new BigInteger(result);
      }
    } catch (NumberFormatException e) {
      log.warn("Could not parse value from output '{}' for source {}", result, source.name());
    }
    return BigInteger.ZERO;
  }
}