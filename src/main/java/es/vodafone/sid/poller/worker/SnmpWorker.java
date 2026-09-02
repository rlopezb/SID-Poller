package es.vodafone.sid.poller.worker;

import es.vodafone.sid.poller.model.Element;
import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Protocol;
import es.vodafone.sid.poller.model.Source;
import es.vodafone.sid.poller.strategy.SourceTypeRegistry;
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

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
  public List<Source> getSources() {
    return sources;
  }

  @Override
  public List<Metric> call() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    Map<Short, Metric> metricMap = new HashMap<>();

    try {
      UserTarget<UdpAddress> target = new UserTarget<>();
      target.setAddress(new UdpAddress(element.name() + "/" + protocol.config().get("port").asInt(161)));
      target.setRetries(1);
      target.setTimeout(5000);
      target.setVersion(SnmpConstants.version3);

      String securityLevel = protocol.config().get("securityLevel").asString("authPriv");
      target.setSecurityLevel(switch (securityLevel.toUpperCase()) {
        case "AUTHNOPRIV" -> SecurityLevel.AUTH_NOPRIV;
        case "AUTHPRIV" -> SecurityLevel.AUTH_PRIV;
        default -> SecurityLevel.NOAUTH_NOPRIV;
      });
      target.setSecurityName(new OctetString(protocol.config().get("username").asString()));
      snmpUserRegistry.accept(protocol, target.getAddress());

      ScopedPDU pdu = new ScopedPDU();
      pdu.setType(PDU.GET);
      sources.forEach(source -> pdu.add(new VariableBinding(new OID(source.address()))));

      ResponseEvent<?> event = snmp.send(pdu, target);
      if (event == null || event.getResponse() == null) {
        log.warn("No SNMP response from {}", element.name());
        return buildMetrics(sources, metricMap, now);
      }

      PDU response = event.getResponse();
      if (response.getErrorStatus() != PDU.noError) {
        log.warn("SNMP error response from {}: {} (errorIndex={})", element.name(), response.getErrorStatusText(), response.getErrorIndex());
        return buildMetrics(sources, metricMap, now);
      }

      Map<OID, Source> sourceByOid = new HashMap<>();
      sources.forEach(source -> sourceByOid.put(new OID(source.address()), source));

      for (int i = 0; i < response.size(); i++) {
        VariableBinding binding = response.get(i);
        Source source = sourceByOid.get(binding.getOid());
        if (source == null) {
          log.warn("Unexpected OID {} in SNMP response from {} (not requested)", binding.getOid(), element.name());
          continue;
        }

        Variable variable = binding.getVariable();
        if (variable == null || variable.isException()) {
          log.debug("No value for OID {} ({}) on {}: {}", binding.getOid(), source.name(), element.name(), variable);
          continue;
        }

        try {
          List<Metric> parsed = sourceTypeRegistry.get(source.type()).calculate(variable.toString(), List.of(source), now);
          if (parsed != null) {
            parsed.forEach(metric -> metricMap.put(metric.srcId(), metric));
          }
        } catch (RuntimeException e) {
          log.warn("Could not measure source {}", source.name(), e);
        }
      }
      return buildMetrics(sources, metricMap, now);
    } catch (IOException | RuntimeException e) {
      log.error("SNMP request failed to {}", element.name(), e);
      return buildMetrics(sources, metricMap, now);
    }
  }
}