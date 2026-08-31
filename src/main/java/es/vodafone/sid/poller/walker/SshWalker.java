package es.vodafone.sid.poller.walker;

import es.vodafone.sid.poller.model.Element;
import es.vodafone.sid.poller.model.Rule;
import es.vodafone.sid.poller.model.Protocol;
import es.vodafone.sid.poller.model.Source;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SshWalker implements Walker {

  private final short discovererId;
  private final Element element;
  private final List<Rule> rules;
  private final Protocol protocol;
  private final SshClient sshClient;

  public SshWalker(short discovererId, Element element, List<Rule> rules,
                   Protocol protocol, SshClient sshClient) {
    this.discovererId = discovererId;
    this.element = element;
    this.rules = rules;
    this.protocol = protocol;
    this.sshClient = sshClient;
  }

  @Override
  public List<Source> call() {
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

      List<Source> discovered = new ArrayList<>();
      for (Rule rule : rules) {
        String rawValue = executeCommand(session, rule.address(), timeout);
        if (rawValue != null) {
          discovered.addAll(discover(rule, rawValue));
        }
      }
      return discovered;

    } catch (IOException e) {
      log.error("SSH walker connection failed to {}", host, e);
      return List.of();
    }
  }

  private List<Source> discover(Rule rule, String rawValue) {
    List<Source> sources = new ArrayList<>();
    Pattern addressPattern = Pattern.compile(rule.pattern());
    Pattern namePattern = Pattern.compile(rule.name());

    for (String line : rawValue.split("\\n")) {
      line = line.trim();
      if (line.isBlank()) continue;

      Matcher addressMatcher = addressPattern.matcher(line);
      if (!addressMatcher.find()) continue;

      Matcher nameMatcher = namePattern.matcher(line);
      String name = nameMatcher.find() ? nameMatcher.group(1) : line;
      String address = addressMatcher.group(1);

      sources.add(new Source(
          (short) 0,
          name,
          null,
          rule.srcType(),
          element.id(),
          element.elementTypeId(),
          element.siteId(),
          element.cdcId(),
          element.zoneId(),
          element.netId(),
          element.archId(),
          rule.grpId(),
          rule.serviceId(),
          rule.serviceTypeId(),
          rule.collectorId(),
          discovererId,
          address,
          null,
          null,
          BigInteger.ZERO,
          rule.scale(),
          true
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