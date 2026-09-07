package es.vodafone.sid.poller.finder;

import es.vodafone.sid.poller.model.*;
import es.vodafone.sid.poller.repository.ElementRepository;
import es.vodafone.sid.poller.repository.ProtocolRepository;
import es.vodafone.sid.poller.repository.RuleRepository;
import es.vodafone.sid.poller.service.WalkerService;
import es.vodafone.sid.poller.walker.SshWalker;
import es.vodafone.sid.poller.walker.Walker;
import org.apache.sshd.client.SshClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SshFinder extends Finder {
  public final SshClient sshClient;
  public SshFinder(ElementRepository elementRepository, RuleRepository ruleRepository, Discoverer discoverer, ProtocolRepository protocolRepository, WalkerService walkerService, SshClient sshClient) {
    super(elementRepository, ruleRepository, discoverer, protocolRepository, walkerService);
    this.sshClient = sshClient;
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

      walkers.add(new SshWalker(discoverer.id(), element, rules, protocol, sshClient));
    }
    return walkerService.run(walkers);
  }
}
