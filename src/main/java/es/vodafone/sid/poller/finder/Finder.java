package es.vodafone.sid.poller.finder;

import es.vodafone.sid.poller.model.Discoverer;
import es.vodafone.sid.poller.model.Source;
import es.vodafone.sid.poller.repository.ElementRepository;
import es.vodafone.sid.poller.repository.ProtocolRepository;
import es.vodafone.sid.poller.repository.RuleRepository;
import es.vodafone.sid.poller.service.WalkerService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.concurrent.Callable;

@RequiredArgsConstructor
public abstract class Finder implements Callable<List<Source>> {
  public final ElementRepository elementRepository;
  public final RuleRepository ruleRepository;
  public final Discoverer discoverer;
  public final ProtocolRepository protocolRepository;
  public final WalkerService walkerService;
}
