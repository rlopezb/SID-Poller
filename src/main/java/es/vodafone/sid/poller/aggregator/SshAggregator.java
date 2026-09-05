package es.vodafone.sid.poller.aggregator;

import es.vodafone.sid.poller.model.*;
import es.vodafone.sid.poller.repository.ElementRepository;
import es.vodafone.sid.poller.repository.ProtocolRepository;
import es.vodafone.sid.poller.repository.SourceRepository;
import es.vodafone.sid.poller.service.WorkerService;
import es.vodafone.sid.poller.strategy.SourceTypeRegistry;
import es.vodafone.sid.poller.worker.SshWorker;
import es.vodafone.sid.poller.worker.Worker;
import org.apache.sshd.client.SshClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SshAggregator extends Aggregator {
  private final SshClient sshClient;

  public SshAggregator(Collector collector,
                       WorkerService workerService,
                       ElementRepository elementRepository,
                       SourceRepository sourceRepository,
                       ProtocolRepository protocolRepository,
                       SshClient sshClient,
                       SourceTypeRegistry sourceTypeRegistry) {
    super(collector, workerService, elementRepository, sourceRepository, protocolRepository, sourceTypeRegistry);
    this.sshClient = sshClient;
  }

  @Override
  public List<Metric> call() {
    List<Source> sources = sourceRepository.findByCollectorId(collector.id());
    Map<Short, Protocol> protocolCache = new HashMap<>();

    List<Worker> workers = new ArrayList<>();
    for (List<Source> group : groupByElement(sources)) {
      Element element = elementRepository.findById(group.getFirst().elementId());
      short elementTypeId = element.elementTypeId();
      Protocol protocol = protocolCache.computeIfAbsent(elementTypeId,
          id -> protocolRepository.getByProtocolAndElementTypeId(collector.protocol(), id));
      workers.add(new SshWorker(element, group, protocol, sshClient, sourceTypeRegistry));
    }
    return workerService.run(workers);
  }
}