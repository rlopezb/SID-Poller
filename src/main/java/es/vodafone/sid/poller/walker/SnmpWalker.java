package es.vodafone.sid.poller.walker;

import es.vodafone.sid.poller.model.Element;
import es.vodafone.sid.poller.model.Protocol;
import es.vodafone.sid.poller.model.Rule;
import es.vodafone.sid.poller.model.Source;
import es.vodafone.sid.poller.rule.MultiRuleType;
import es.vodafone.sid.poller.rule.RuleTypeRegistry;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.Snmp;
import org.snmp4j.UserTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Slf4j
public class SnmpWalker extends Walker {
  private final Snmp snmp;
  private final BiConsumer<Protocol, UdpAddress> snmpUserRegistry;

  public SnmpWalker(short discovererId, Element element, List<Rule> rules,
                    Protocol protocol, Snmp snmp, BiConsumer<Protocol, UdpAddress> snmpUserRegistry, RuleTypeRegistry ruleTypeRegistry) {
    super(discovererId, element, rules, protocol, ruleTypeRegistry);
    this.snmp = snmp;
    this.snmpUserRegistry = snmpUserRegistry;
  }

  @Override
  public List<Source> call() {
    JsonNode config = protocol.config();
    int port = config.get("port").asInt(161);
    String username = config.get("username").asString();
    String securityLevel = config.get("securityLevel").asString("authPriv");

    UserTarget<UdpAddress> target = new UserTarget<>();
    target.setAddress(new UdpAddress(element.name() + "/" + port));
    target.setRetries(protocol.config().get("retries").asInt(1));
    target.setTimeout(protocol.config().get("timeout").asInt(1000));
    target.setVersion(SnmpConstants.version3);
    target.setSecurityLevel(switch (securityLevel.toUpperCase()) {
      case "AUTHNOPRIV" -> SecurityLevel.AUTH_NOPRIV;
      case "AUTHPRIV" -> SecurityLevel.AUTH_PRIV;
      default -> SecurityLevel.NOAUTH_NOPRIV;
    });
    target.setSecurityName(new OctetString(username));
    snmpUserRegistry.accept(protocol, target.getAddress());
    TableUtils tableUtils = new TableUtils(snmp, new DefaultPDUFactory());

    List<Source> discovered = new ArrayList<>();
    for (Rule rule : rules) {
      OID[] searchOIDs = new OID[rule.search().length];
      for (int i = 0; i < rule.search().length; i++) {
        searchOIDs[i] = new OID(rule.search()[i]);
      }
      List<TableEvent> events = tableUtils.getTable(target, searchOIDs, null, null);
      for (TableEvent event : events) {
        if (event == null || event.isError()) {
          log.warn("SNMP walk error on {} for search OIDs {}: {}",
              element.name(), rule.search(),
              event != null ? event.getErrorMessage() : "null event");
          continue;
        }

        VariableBinding[] vbs = event.getColumns();
        if (vbs == null) continue;
        Map<OID, String> results = new LinkedHashMap<>();
        for (VariableBinding vb : vbs) {
          results.put(vb.getOid(), vb.getVariable().toString());
        }
        List<Source> sources = ((MultiRuleType) ruleTypeRegistry.get(rule.type())).calculate(rule, element, results);
        discovered.addAll(sources);
      }
    }
    return discovered;
  }
}