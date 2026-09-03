package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.aggregator.Aggregator;
import es.vodafone.sid.poller.model.*;
import es.vodafone.sid.poller.repository.ElementRepository;
import es.vodafone.sid.poller.repository.ProtocolRepository;
import es.vodafone.sid.poller.repository.SourceRepository;
import es.vodafone.sid.poller.strategy.SourceTypeRegistry;
import es.vodafone.sid.poller.worker.SnmpWorker;
import es.vodafone.sid.poller.worker.SshWorker;
import es.vodafone.sid.poller.worker.Worker;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.client.SshClient;
import org.snmp4j.Snmp;
import org.snmp4j.smi.UdpAddress;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AggregatorFactory {
  private final ElementRepository elementRepository;
  private final SourceRepository sourceRepository;
  private final ProtocolRepository protocolRepository;
  private final SshClient sshClient;
  private final Snmp snmp;
  private final BiConsumer<Protocol, UdpAddress> snmpUserRegistry;
  private final SourceTypeRegistry sourceTypeRegistry;

  public Aggregator create(Collector collector, WorkerService workerService) {
    return switch (collector.protocol().toUpperCase()) {
      case "SSH"  -> () -> collectSsh(collector, workerService);
      case "SNMP" -> () -> collectSnmp(collector, workerService);
      default -> throw new IllegalArgumentException("Unknown protocol: " + collector.protocol());
    };
  }

  private List<Metric> collectSsh(Collector collector, WorkerService workerService) {
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

  private List<Metric> collectSnmp(Collector collector, WorkerService workerService) {
    List<Source> sources = sourceRepository.findByCollectorId(collector.id());
    Map<Short, Protocol> protocolCache = new HashMap<>();

    List<Worker> workers = new ArrayList<>();
    for (List<Source> group : groupByElement(sources)) {
      Element element = elementRepository.findById(group.getFirst().elementId());
      short elementTypeId = element.elementTypeId();
      Protocol protocol = protocolCache.computeIfAbsent(elementTypeId,
          id -> protocolRepository.getByProtocolAndElementTypeId(collector.protocol(), id));
      int maxOid = protocol.config().get("maxOid").asInt();
      if(maxOid==0) maxOid = group.size();
      for (List<Source> chunk : partition(group, maxOid)) {
        workers.add(new SnmpWorker(element, chunk, protocol, snmp, snmpUserRegistry, sourceTypeRegistry));
      }
    }
    return workerService.run(workers);
  }

  private static Collection<List<Source>> groupByElement(List<Source> sources) {
    return sources.stream()
        .collect(Collectors.groupingBy(Source::elementId))
        .values();
  }

  private static <T> List<List<T>> partition(List<T> list, int size) {
    List<List<T>> partitions = new ArrayList<>();
    for (int i = 0; i < list.size(); i += size) {
      partitions.add(list.subList(i, Math.min(i + size, list.size())));
    }
    return partitions;
  }
}