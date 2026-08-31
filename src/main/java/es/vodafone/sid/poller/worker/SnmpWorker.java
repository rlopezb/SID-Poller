package es.vodafone.sid.poller.worker;

import es.vodafone.sid.poller.model.Element;
import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Protocol;
import es.vodafone.sid.poller.model.Source;
import es.vodafone.sid.poller.strategy.SourceTypeRegistry;
import es.vodafone.sid.poller.strategy.BaseSourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.*;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
public class SnmpWorker implements Worker {
  private final Element element;
  private final List<Source> sources;
  private final Protocol protocol;
  private final Snmp snmp;
  private final BiConsumer<Protocol, UdpAddress> snmpUserRegistry;
  private final SourceTypeRegistry sourceTypeRegistry;

  @Override
  public List<Metric> call() {

    JsonNode config = protocol.config();
    int port = config.get("port").asInt(161);
    String username = config.get("username").asString();
    String securityLevel = config.get("securityLevel").asString("authPriv");
    Target<UdpAddress> target = buildTarget(element.name(), port, username, securityLevel);
    snmpUserRegistry.accept(protocol, target.getAddress());
    PDU pdu = buildPdu(sources);
    OffsetDateTime instant = OffsetDateTime.now(ZoneOffset.UTC);
    try {
      ResponseEvent<?> event = snmp.send(pdu, target);
      if (event == null || event.getResponse() == null) {
        log.warn("No SNMP response from {}", element.name());
        return sources.stream()
            .map(source -> BaseSourceType.nullMetric(source, instant))
            .toList();
      }
      PDU response = event.getResponse();
      List<Metric> metrics = new ArrayList<>();
      for (int i = 0; i < response.size(); i++) {
        String rawValue = response.get(i).getVariable().toString();
        Source source = sources.get(i);
        metrics.addAll(sourceTypeRegistry.get(source.type()).apply(rawValue, List.of(source), instant));
      }
      return metrics;

    } catch (IOException e) {
      log.error("SNMP request failed to {}", element.name(), e);
      return sources.stream()
          .map(source -> BaseSourceType.nullMetric(source, instant))
          .toList();
    }
  }
  private Target<UdpAddress> buildTarget(String host, int port,
                                         String username, String securityLevel) {
    UserTarget<UdpAddress> target = new UserTarget<>();
    target.setAddress(new UdpAddress(host + "/" + port));
    target.setRetries(1);
    target.setTimeout(5000);
    target.setVersion(SnmpConstants.version3);
    target.setSecurityLevel(resolveSecurityLevel(securityLevel));
    target.setSecurityName(new OctetString(username));
    return target;
  }

  private PDU buildPdu(List<Source> sources) {
    ScopedPDU pdu = new ScopedPDU();
    pdu.setType(PDU.GET);
    sources.forEach(source -> pdu.add(new VariableBinding(new OID(source.address()))));
    return pdu;
  }

  private int resolveSecurityLevel(String level) {
    return switch (level.toUpperCase()) {
      case "AUTHNOPRIV" -> SecurityLevel.AUTH_NOPRIV;
      case "AUTHPRIV"   -> SecurityLevel.AUTH_PRIV;
      default           -> SecurityLevel.NOAUTH_NOPRIV;
    };
  }
}