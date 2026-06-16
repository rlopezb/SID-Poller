package es.vodafone.sid.poller.walker;

import es.vodafone.sid.poller.model.ElementRecord;
import es.vodafone.sid.poller.model.PatternRecord;
import es.vodafone.sid.poller.model.ProtocolRecord;
import es.vodafone.sid.poller.model.SourceRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SshWalker implements Callable<List<SourceRecord>> {

  private final short discovererId;
  private final ElementRecord element;
  private final List<PatternRecord> patterns;
  private final ProtocolRecord protocol;
  private final SshClient sshClient;

  public SshWalker(short discovererId, ElementRecord element, List<PatternRecord> patterns,
                   ProtocolRecord protocol, SshClient sshClient) {
    this.discovererId = discovererId;
    this.element = element;
    this.patterns = patterns;
    this.protocol = protocol;
    this.sshClient = sshClient;
  }

  @Override
  public List<SourceRecord> call() {
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

      List<SourceRecord> discovered = new ArrayList<>();
      for (PatternRecord pattern : patterns) {
        String rawValue = executeCommand(session, pattern.address(), timeout);
        if (rawValue != null) {
          discovered.addAll(discover(pattern, rawValue));
        }
      }
      return discovered;

    } catch (IOException e) {
      log.error("SSH walker connection failed to {}", host, e);
      return List.of();
    }
  }

  private List<SourceRecord> discover(PatternRecord pattern, String rawValue) {
    List<SourceRecord> sources = new ArrayList<>();
    Pattern addressPattern = Pattern.compile(pattern.pattern());
    Pattern namePattern = Pattern.compile(pattern.name());

    for (String line : rawValue.split("\\n")) {
      line = line.trim();
      if (line.isBlank()) continue;

      Matcher addressMatcher = addressPattern.matcher(line);
      if (!addressMatcher.find()) continue;

      Matcher nameMatcher = namePattern.matcher(line);
      String name = nameMatcher.find() ? nameMatcher.group(1) : line;
      String address = addressMatcher.group(1);

      sources.add(new SourceRecord(
          (short) 0,
          name,
          null,
          pattern.srcType(),
          element.id(),
          element.elementTypeId(),
          element.siteId(),
          element.cdcId(),
          element.zoneId(),
          element.netId(),
          element.archId(),
          pattern.grpId(),
          pattern.serviceId(),
          pattern.serviceTypeId(),
          pattern.collectorId(),
          discovererId,
          address,
          null,
          null,
          0.0
      ));
    }
    return sources;
  }

  private String executeCommand(ClientSession session, String command, long timeout) {
    log.debug("Executing ssh walker command: {}", command);
    try (ChannelExec channel = session.createExecChannel(command)) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      channel.setOut(output);
      channel.open().verify(timeout, TimeUnit.MILLISECONDS);
      channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), timeout);
      return output.toString(StandardCharsets.UTF_8).trim();
    } catch (IOException e) {
      log.error("Command '{}' failed on {}", command, element.name(), e);
      return null;
    }
  }
}