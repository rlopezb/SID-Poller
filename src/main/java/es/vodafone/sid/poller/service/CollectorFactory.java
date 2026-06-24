package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.*;
import es.vodafone.sid.poller.repository.ElementRepository;
import es.vodafone.sid.poller.repository.ProtocolRepository;
import es.vodafone.sid.poller.repository.SourceRepository;
import es.vodafone.sid.poller.strategy.SourceTypeRegistry;
import es.vodafone.sid.poller.worker.SnmpWorker;
import es.vodafone.sid.poller.worker.SshWorker;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.client.SshClient;
import org.snmp4j.Snmp;
import org.snmp4j.smi.UdpAddress;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CollectorFactory {
  private final ElementRepository elementRepository;
  private final SourceRepository sourceRepository;
  private final ProtocolRepository protocolRepository;
  private final SshClient sshClient;
  private final Snmp snmp;
  private final BiConsumer<ProtocolRecord, UdpAddress> snmpUserRegistry;
  private final SourceTypeRegistry sourceTypeRegistry;

  public Callable<List<MetricRecord>> create(CollectorRecord collector, WorkersService workersService) {
    return switch (collector.protocol().toUpperCase()) {
      case "SSH"  -> () -> collectSsh(collector, workersService);
      case "SNMP" -> () -> collectSnmp(collector, workersService);
      default -> throw new IllegalArgumentException("Unknown protocol: " + collector.protocol());
    };
  }

  private List<MetricRecord> collectSsh(CollectorRecord collector, WorkersService workersService) {
    List<SourceRecord> sources = sourceRepository.findByCollectorId(collector.id());
    Map<Short, ProtocolRecord> protocolCache = new HashMap<>();

    List<Callable<List<MetricRecord>>> workers = new ArrayList<>();
    for (List<SourceRecord> group : groupByElement(sources)) {
      ElementRecord element = elementRepository.findById(group.getFirst().elementId());
      short elementTypeId = element.elementTypeId();
      ProtocolRecord protocol = protocolCache.computeIfAbsent(elementTypeId,
          id -> protocolRepository.getByProtocolAndElementTypeId(collector.protocol(), id));
      workers.add(new SshWorker(element, group, protocol, sshClient, sourceTypeRegistry));
    }
    return workersService.get(workers);
  }

  private List<MetricRecord> collectSnmp(CollectorRecord collector, WorkersService workersService) {
    List<SourceRecord> sources = sourceRepository.findByCollectorId(collector.id());
    Map<Short, ProtocolRecord> protocolCache = new HashMap<>();

    List<Callable<List<MetricRecord>>> workers = new ArrayList<>();
    for (List<SourceRecord> group : groupByElement(sources)) {
      ElementRecord element = elementRepository.findById(group.getFirst().elementId());
      short elementTypeId = element.elementTypeId();
      ProtocolRecord protocol = protocolCache.computeIfAbsent(elementTypeId,
          id -> protocolRepository.getByProtocolAndElementTypeId(collector.protocol(), id));
      int maxOid = protocol.config().get("maxOid").asInt();
      for (List<SourceRecord> chunk : partition(group, maxOid)) {
        workers.add(new SnmpWorker(element, chunk, protocol, snmp, snmpUserRegistry, sourceTypeRegistry));
      }
    }
    return workersService.get(workers);
  }

  private static Collection<List<SourceRecord>> groupByElement(List<SourceRecord> sources) {
    return sources.stream()
        .collect(Collectors.groupingBy(SourceRecord::elementId))
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