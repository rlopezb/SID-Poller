package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.finder.Finder;
import es.vodafone.sid.poller.finder.SnmpFinder;
import es.vodafone.sid.poller.finder.SshFinder;
import es.vodafone.sid.poller.model.Discoverer;
import es.vodafone.sid.poller.model.Protocol;
import es.vodafone.sid.poller.repository.ElementRepository;
import es.vodafone.sid.poller.repository.ProtocolRepository;
import es.vodafone.sid.poller.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.client.SshClient;
import org.snmp4j.Snmp;
import org.snmp4j.smi.UdpAddress;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
public class FinderFactory {

  private final ElementRepository elementRepository;
  private final RuleRepository ruleRepository;
  private final ProtocolRepository protocolRepository;
  private final SshClient sshClient;
  private final Snmp snmp;
  private final BiConsumer<Protocol, UdpAddress> snmpUserRegistry;

  public Finder create(Discoverer discoverer, WalkerService walkerService) {
    return switch (discoverer.protocol().toUpperCase()) {
      case "SSH" -> new SshFinder(elementRepository, ruleRepository, discoverer,
          protocolRepository, walkerService, sshClient);
      case "SNMP" -> new SnmpFinder(elementRepository, ruleRepository, discoverer,
          protocolRepository, walkerService, snmp, snmpUserRegistry);
      default -> throw new IllegalArgumentException("Unknown protocol: " + discoverer.protocol());
    };
  }
}