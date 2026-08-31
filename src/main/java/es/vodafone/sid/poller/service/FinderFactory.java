package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.finder.Finder;
import es.vodafone.sid.poller.model.*;
import es.vodafone.sid.poller.repository.ElementRepository;
import es.vodafone.sid.poller.repository.RuleRepository;
import es.vodafone.sid.poller.repository.ProtocolRepository;
import es.vodafone.sid.poller.walker.SnmpWalker;
import es.vodafone.sid.poller.walker.SshWalker;
import es.vodafone.sid.poller.walker.Walker;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.client.SshClient;
import org.snmp4j.Snmp;
import org.snmp4j.smi.UdpAddress;
import org.springframework.stereotype.Component;

import java.util.*;
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
      case "SSH"  -> () -> walkSsh(discoverer, walkerService);
      case "SNMP" -> () -> walkSnmp(discoverer, walkerService);
      default -> throw new IllegalArgumentException("Unknown protocol: " + discoverer.protocol());
    };
  }

  private List<Source> walkSsh(Discoverer discoverer, WalkerService walkerService) {
    List<Element> elements = elementRepository.findAll();
    Map<Short, Protocol> protocolCache = new HashMap<>();

    List<Walker> walkers = new ArrayList<>();
    for (Element element : elements) {
      List<Rule> rules = ruleRepository
          .findByDiscovererAndElementTypeId(discoverer.protocol(), element.elementTypeId());
      if (rules.isEmpty()) continue;

      Protocol protocol = protocolCache.computeIfAbsent(element.elementTypeId(),
          id -> protocolRepository.getByProtocolAndElementTypeId(discoverer.protocol(), id));

      walkers.add(new SshWalker(discoverer.id(), element, rules, protocol, sshClient));
    }
    return walkerService.get(walkers);
  }

  private List<Source> walkSnmp(Discoverer discoverer, WalkerService walkerService) {
    List<Element> elements = elementRepository.findAll();
    Map<Short, Protocol> protocolCache = new HashMap<>();

    List<Walker> walkers = new ArrayList<>();
    for (Element element : elements) {
      List<Rule> rules = ruleRepository
          .findByDiscovererAndElementTypeId(discoverer.protocol(), element.elementTypeId());
      if (rules.isEmpty()) continue;

      Protocol protocol = protocolCache.computeIfAbsent(element.elementTypeId(),
          id -> protocolRepository.getByProtocolAndElementTypeId(discoverer.protocol(), id));

      walkers.add(new SnmpWalker(discoverer.id(), element, rules, protocol, snmp, snmpUserRegistry));
    }
    return walkerService.get(walkers);
  }
}