package es.vodafone.sid.poller.configuration;

import es.vodafone.sid.poller.model.ProtocolRecord;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.*;

import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
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
import java.util.function.BiConsumer;

@Configuration
@Slf4j
public class SnmpClientConfiguration {
  @Bean
  public USM usm() {
    OctetString localEngineId = new OctetString(MPv3.createLocalEngineID());
    USM usm = new USM(SecurityProtocols.getInstance(), localEngineId, 0);
    usm.setEngineDiscoveryEnabled(true);
    return usm;
  }

  @Bean(destroyMethod = "close")
  public Snmp snmp(USM usm) throws IOException {
    SecurityProtocols.getInstance().addDefaultProtocols();

    SecurityModels securityModels = new SecurityModels();
    securityModels.addSecurityModel(usm);

    MPv3 mpv3 = new MPv3(usm.getLocalEngineID().getValue());
    mpv3.setSecurityModels(securityModels);

    MessageDispatcher dispatcher = new MessageDispatcherImpl();
    dispatcher.addMessageProcessingModel(new MPv1());
    dispatcher.addMessageProcessingModel(new MPv2c());
    dispatcher.addMessageProcessingModel(mpv3);

    TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
    Snmp snmp = new Snmp(dispatcher, transport);
    transport.listen();
    return snmp;
  }

  @Bean
  public BiConsumer<ProtocolRecord, UdpAddress> snmpUserRegistry(USM usm) {
    Set<String> registeredUsers = ConcurrentHashMap.newKeySet();
    return (protocol, address) -> {
      JsonNode config = protocol.config();
      String username = config.get("username").asString();
      String key = username + ":"
          + config.get("authProtocol").asString() + ":"
          + config.get("privProtocol").asString();
      if (registeredUsers.add(key)) {
        usm.addUser(               // ← directamente sobre el USM bean
            new OctetString(username),
            null,
            new UsmUser(
                new OctetString(username),
                resolveAuthProtocol(config.get("authProtocol").asString()),
                new OctetString(config.get("authPassword").asString()),
                resolvePrivProtocol(config.get("privProtocol").asString()),
                new OctetString(config.get("privPassword").asString())
            )
        );
        log.debug("Registered SNMP user: {}", key);
      }
    };
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
      case "AES", "AES-128" -> PrivAES128.ID;
      case "AES-256" -> PrivAES256.ID;
      default -> PrivDES.ID;
    };
  }
}

