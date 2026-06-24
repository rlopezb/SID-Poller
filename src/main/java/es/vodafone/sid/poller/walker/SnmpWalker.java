package es.vodafone.sid.poller.walker;

import es.vodafone.sid.poller.model.ElementRecord;
import es.vodafone.sid.poller.model.PatternRecord;
import es.vodafone.sid.poller.model.ProtocolRecord;
import es.vodafone.sid.poller.model.SourceRecord;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SnmpWalker implements Callable<List<SourceRecord>> {

  private final short discovererId;
  private final ElementRecord element;
  private final List<PatternRecord> patterns;
  private final ProtocolRecord protocol;
  private final Snmp snmp;
  private final BiConsumer<ProtocolRecord, UdpAddress> snmpUserRegistry;

  public SnmpWalker(short discovererId, ElementRecord element, List<PatternRecord> patterns,
                    ProtocolRecord protocol, Snmp snmp, BiConsumer<ProtocolRecord, UdpAddress> snmpUserRegistry) {
    this.discovererId = discovererId;
    this.element = element;
    this.patterns = patterns;
    this.protocol = protocol;
    this.snmp = snmp;
    this.snmpUserRegistry = snmpUserRegistry;
  }

  @Override
  public List<SourceRecord> call() {
    JsonNode config = protocol.config();
    int port = config.get("port").asInt(161);
    String username = config.get("username").asString();
    String securityLevel = config.get("securityLevel").asString("authPriv");

    Target<UdpAddress> target = buildTarget(element.name(), port, username, securityLevel);
    snmpUserRegistry.accept(protocol, target.getAddress());
    TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());

    List<SourceRecord> discovered = new ArrayList<>();
    for (PatternRecord pattern : patterns) {
      List<SourceRecord> sources = walk(treeUtils, target, pattern);
      discovered.addAll(sources);
    }
    return discovered;
  }

  private List<SourceRecord> walk(TreeUtils treeUtils, Target<UdpAddress> target, PatternRecord pattern) {
    List<SourceRecord> sources = new ArrayList<>();
    Pattern addressPattern = Pattern.compile(pattern.pattern());
    Pattern namePattern = Pattern.compile(pattern.name());

    List<TreeEvent> events = treeUtils.getSubtree(target, new OID(pattern.address()));
    for (TreeEvent event : events) {
      if (event == null || event.isError()) {
        log.warn("SNMP walk error on {} for OID {}: {}",
            element.name(), pattern.address(),
            event != null ? event.getErrorMessage() : "null event");
        continue;
      }

      VariableBinding[] vbs = event.getVariableBindings();
      if (vbs == null) continue;

      for (VariableBinding vb : vbs) {
        String oid = vb.getOid().toString();
        String value = vb.getVariable().toString();

        // El pattern se aplica al valor devuelto
        Matcher addressMatcher = addressPattern.matcher(value);
        if (!addressMatcher.find()) continue;

        Matcher nameMatcher = namePattern.matcher(value);
        String name = nameMatcher.find() ? nameMatcher.group(1) : value;
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
    }
    return sources;
  }

  private Target<UdpAddress> buildTarget(String host, int port,
                                         String username, String securityLevel) {
    org.snmp4j.UserTarget<UdpAddress> target = new org.snmp4j.UserTarget<>();
    target.setAddress(new UdpAddress(host + "/" + port));
    target.setRetries(1);
    target.setTimeout(5000);
    target.setVersion(SnmpConstants.version3);
    target.setSecurityLevel(resolveSecurityLevel(securityLevel));
    target.setSecurityName(new OctetString(username));
    return target;
  }

  private int resolveSecurityLevel(String level) {
    return switch (level.toUpperCase()) {
      case "AUTHNOPRIV" -> SecurityLevel.AUTH_NOPRIV;
      case "AUTHPRIV"   -> SecurityLevel.AUTH_PRIV;
      default           -> SecurityLevel.NOAUTH_NOPRIV;
    };
  }
}