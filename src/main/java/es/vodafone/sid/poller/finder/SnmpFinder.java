package es.vodafone.sid.poller.finder;

import es.vodafone.sid.poller.model.*;
import es.vodafone.sid.poller.repository.ElementRepository;
import es.vodafone.sid.poller.repository.ProtocolRepository;
import es.vodafone.sid.poller.repository.RuleRepository;
import es.vodafone.sid.poller.service.WalkerService;
import es.vodafone.sid.poller.walker.SnmpWalker;
import es.vodafone.sid.poller.walker.Walker;
import org.snmp4j.Snmp;
import org.snmp4j.smi.UdpAddress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class SnmpFinder extends Finder {
  public final Snmp snmp;
  private final BiConsumer<Protocol, UdpAddress> snmpUserRegistry;

  public SnmpFinder(ElementRepository elementRepository, RuleRepository ruleRepository, Discoverer discoverer, ProtocolRepository protocolRepository, WalkerService walkerService, Snmp snmp, BiConsumer<Protocol, UdpAddress> snmpUserRegistry) {
    super(elementRepository, ruleRepository, discoverer, protocolRepository, walkerService);
    this.snmp = snmp;
    this.snmpUserRegistry = snmpUserRegistry;
  }

  @Override
  public List<Source> call() {
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
    return walkerService.run(walkers);
  }
}
