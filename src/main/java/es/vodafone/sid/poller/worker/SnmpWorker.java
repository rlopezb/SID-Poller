package es.vodafone.sid.poller.worker;

import es.vodafone.sid.poller.model.ElementRecord;
import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.model.ProtocolRecord;
import es.vodafone.sid.poller.model.SourceRecord;
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
import java.util.concurrent.Callable;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class SnmpWorker implements Callable<List<MetricRecord>> {
  private final ElementRecord element;
  private final List<SourceRecord> sources;
  private final ProtocolRecord protocol;
  private final Snmp snmp;
  private final Consumer<ProtocolRecord> snmpUserRegistry;
  private final SourceTypeRegistry sourceTypeRegistry;

  @Override
  public List<MetricRecord> call() {
    snmpUserRegistry.accept(protocol);
    JsonNode config = protocol.config();
    int port = config.get("port").asInt(161);
    String username = config.get("username").asString();
    String securityLevel = config.get("securityLevel").asString("authPriv");

    Target<UdpAddress> target = buildTarget(element.name(), port, username, securityLevel);
    PDU pdu = buildPdu(sources);
    OffsetDateTime instant = OffsetDateTime.now(ZoneOffset.UTC);
    try {
      log.debug("Sending PDU with size: {}", pdu.size());
      ResponseEvent<?> event = snmp.send(pdu, target);
      if (event == null || event.getResponse() == null) {
        log.warn("No SNMP response from {}", element.name());
        return sources.stream()
            .map(source -> BaseSourceType.nullMetric(source, instant))
            .toList();
      }
      PDU response = event.getResponse();
      List<MetricRecord> metrics = new ArrayList<>();
      for (int i = 0; i < response.size(); i++) {
        String rawValue = response.get(i).getVariable().toString();
        SourceRecord source = sources.get(i);
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

  private PDU buildPdu(List<SourceRecord> sources) {
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