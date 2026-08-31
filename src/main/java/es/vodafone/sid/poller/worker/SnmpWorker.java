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
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    int port = protocol.config().get("port").asInt(161);
    String username = protocol.config().get("username").asString();
    String securityLevel = protocol.config().get("securityLevel").asString("authPriv");
    Target<UdpAddress> target = buildTarget(element.name(), port, username, securityLevel);
    snmpUserRegistry.accept(protocol, target.getAddress());
    PDU pdu = buildPdu(sources);
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    Map<Short, Metric> metricMap = new HashMap<>();

    try {
      ResponseEvent<?> event = snmp.send(pdu, target);
      if (event == null || event.getResponse() == null) {
        log.warn("No SNMP response from {}", element.name());
        return buildMetrics(metricMap, now);
      }
      PDU response = event.getResponse();
      if (response.getErrorStatus() != PDU.noError) {
        log.warn("SNMP error response from {}: {} (errorIndex={})", element.name(), response.getErrorStatusText(), response.getErrorIndex());
        return buildMetrics(metricMap, now);
      }

      Map<OID, Source> sourceByOid = new HashMap<>();
      sources.forEach(source -> sourceByOid.put(new OID(source.address()), source));

      for (int i = 0; i < response.size(); i++) {
        VariableBinding vb = response.get(i);
        OID oid = vb.getOid();
        Source source = sourceByOid.get(oid);
        if (source == null) {
          log.warn("Unexpected OID {} in SNMP response from {} (not requested)", oid, element.name());
          continue;
        }

        Variable variable = vb.getVariable();
        if (variable == null || variable.isException()) {
          log.debug("No value for OID {} ({}) on {}: {}", oid, source.name(), element.name(), variable);
          continue;
        }

        try {
          String rawValue = variable.toString();
          List<Metric> parsed = sourceTypeRegistry.get(source.type()).apply(rawValue, List.of(source), now);
          if (parsed != null) {
            parsed.forEach(metric -> metricMap.put(metric.srcId(), metric));
          }
        } catch (RuntimeException e) {
          log.warn("Could not measure source {}", source.name(), e);
        }
      }
      return buildMetrics(metricMap, now);

    } catch (IOException | RuntimeException e) {
      log.error("SNMP request failed to {}", element.name(), e);
      return buildMetrics(metricMap, now);
    }
  }

  private List<Metric> buildMetrics(Map<Short, Metric> metricMap, OffsetDateTime instant) {
    List<Metric> metrics = new ArrayList<>();
    for (Source source : sources) {
      metrics.add(metricMap.getOrDefault(source.id(), BaseSourceType.nullMetric(source, instant)));
    }
    return metrics;
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