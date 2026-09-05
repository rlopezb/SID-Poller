package es.vodafone.sid.poller.aggregator;

import es.vodafone.sid.poller.model.Collector;
import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Source;
import es.vodafone.sid.poller.repository.ElementRepository;
import es.vodafone.sid.poller.repository.ProtocolRepository;
import es.vodafone.sid.poller.repository.SourceRepository;
import es.vodafone.sid.poller.service.WorkerService;
import es.vodafone.sid.poller.strategy.SourceTypeRegistry;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class Aggregator implements Callable<List<Metric>> {
  public final Collector collector;
  public final WorkerService workerService;
  public final ElementRepository elementRepository;
  public final SourceRepository sourceRepository;
  public final ProtocolRepository protocolRepository;
  public final SourceTypeRegistry sourceTypeRegistry;

  protected static Collection<List<Source>> groupByElement(List<Source> sources) {
    return sources.stream().collect(Collectors.groupingBy(Source::elementId)).values();
  }
}
