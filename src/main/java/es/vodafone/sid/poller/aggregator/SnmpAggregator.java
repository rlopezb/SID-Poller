package es.vodafone.sid.poller.aggregator;

import es.vodafone.sid.poller.model.*;
import es.vodafone.sid.poller.repository.ElementRepository;
import es.vodafone.sid.poller.repository.ProtocolRepository;
import es.vodafone.sid.poller.repository.SourceRepository;
import es.vodafone.sid.poller.service.WorkerService;
import es.vodafone.sid.poller.strategy.SourceTypeRegistry;
import es.vodafone.sid.poller.worker.SnmpWorker;
import es.vodafone.sid.poller.worker.Worker;
import org.snmp4j.Snmp;
import org.snmp4j.smi.UdpAddress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class SnmpAggregator extends Aggregator {
  private final Snmp snmp;
  private final BiConsumer<Protocol, UdpAddress> snmpUserRegistry;

  public SnmpAggregator(Collector collector, WorkerService workerService, ElementRepository elementRepository, SourceRepository sourceRepository, ProtocolRepository protocolRepository,
                        Snmp snmp, BiConsumer<Protocol, UdpAddress> snmpUserRegistry,
                        SourceTypeRegistry sourceTypeRegistry) {
    super(collector, workerService, elementRepository, sourceRepository, protocolRepository, sourceTypeRegistry);
    this.snmp = snmp;
    this.snmpUserRegistry = snmpUserRegistry;
  }

  @Override
  public List<Metric> call() {
    List<Source> sources = sourceRepository.findByCollectorId(collector.id());
    Map<Short, Protocol> protocolCache = new HashMap<>();
    List<Worker> workers = new ArrayList<>();
    for (List<Source> group : groupByElement(sources)) {
      Element element = elementRepository.findById(group.getFirst().elementId());
      Protocol protocol = protocolCache.computeIfAbsent(element.elementTypeId(),
          id -> protocolRepository.getByProtocolAndElementTypeId(collector.protocol(), id));
      int maxOid = protocol.config().get("maxOid").asInt();
      if (maxOid == 0) maxOid = group.size();
      for (List<Source> chunk : partition(group, maxOid)) {
        workers.add(new SnmpWorker(element, chunk, protocol, snmp, snmpUserRegistry, sourceTypeRegistry));
      }
    }
    return workerService.run(workers);
  }

  private static <T> List<List<T>> partition(List<T> list, int size) {
    List<List<T>> partitions = new ArrayList<>();
    for (int i = 0; i < list.size(); i += size) {
      partitions.add(list.subList(i, Math.min(i + size, list.size())));
    }
    return partitions;
  }

}