package es.vodafone.sid.poller.configuration;

import es.vodafone.sid.poller.model.ProtocolRecord;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.*;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Configuration
public class SnmpClientConfiguration {
  private final Set<String> registeredUsers = ConcurrentHashMap.newKeySet();

  @Bean(destroyMethod = "close")
  public Snmp snmp() throws IOException {
    TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
    Snmp snmp = new Snmp(transport);
    USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
    SecurityModels.getInstance().addSecurityModel(usm);
    transport.listen();
    return snmp;
  }

  @Bean
  public Consumer<ProtocolRecord> snmpUserRegistry(Snmp snmp) {
    Set<String> registeredUsers = ConcurrentHashMap.newKeySet();
    return protocol -> {
      JsonNode config = protocol.config();
      String username = config.get("username").asString();
      if (registeredUsers.add(username)) {
        snmp.getUSM().addUser(new UsmUser(
            new OctetString(username),
            resolveAuthProtocol(config.get("authProtocol").asString()),
            new OctetString(config.get("authPassword").asString()),
            resolvePrivProtocol(config.get("privProtocol").asString()),
            new OctetString(config.get("privPassword").asString())
        ));
      }
    };
  }

  public void registerUserIfAbsent(Snmp snmp, ProtocolRecord protocol) {
    JsonNode config = protocol.config();
    String username = config.get("username").asString();

    if (registeredUsers.add(username)) {
      snmp.getUSM().addUser(
          new UsmUser(
              new OctetString(username),
              resolveAuthProtocol(config.get("authProtocol").asString()),
              new OctetString(config.get("authPassword").asString()),
              resolvePrivProtocol(config.get("privProtocol").asString()),
              new OctetString(config.get("privPassword").asString())
          )
      );
    }
  }

  private static OID resolveAuthProtocol(String protocol) {
    return switch (protocol.toUpperCase()) {
      case "SHA" -> AuthSHA.ID;
      case "SHA-256" -> AuthHMAC192SHA256.ID;
      case "SHA-512" -> AuthHMAC384SHA512.ID;
      default -> AuthMD5.ID;
    };
  }

  private static OID resolvePrivProtocol(String protocol) {
    return switch (protocol.toUpperCase()) {
      case "AES-128" -> PrivAES128.ID;
      case "AES-256" -> PrivAES256.ID;
      default -> PrivDES.ID;
    };
  }
}

