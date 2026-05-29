package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.CollectorRecord;
import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.model.ProtocolRecord;
import es.vodafone.sid.poller.model.SourceRecord;
import es.vodafone.sid.poller.repository.ProtocolRepository;
import es.vodafone.sid.poller.repository.SourceRepository;
import es.vodafone.sid.poller.worker.SnmpWorker;
import es.vodafone.sid.poller.worker.SshWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CollectorFactory {
  private final SourceRepository sourceRepository;
  private final ProtocolRepository protocolRepository;

  public Callable<List<MetricRecord>> create(CollectorRecord collector, WorkersService workersService) {
    return switch (collector.protocol().toUpperCase()) {
      case "SSH" -> createCollector(collector, workersService, this::createSshWorkers);
      case "SNMP" -> createCollector(collector, workersService, this::createSnmpWorkers);
      default -> throw new IllegalArgumentException("Unknown protocol: " + collector.protocol());
    };
  }

  private Callable<List<MetricRecord>> createCollector(
      CollectorRecord collector,
      WorkersService workersService,
      BiFunction<List<SourceRecord>, ProtocolRecord, List<Callable<List<MetricRecord>>>> workerBuilder) {
    return () -> {
      List<SourceRecord> sources = sourceRepository.findAllByCollectorId(collector.id());
      Map<Short, ProtocolRecord> protocolCache = new HashMap<>();
      Map<Short, List<SourceRecord>> byElement = sources.stream()
          .collect(Collectors.groupingBy(SourceRecord::elementId));

      List<Callable<List<MetricRecord>>> workers = byElement.entrySet().stream()
          .flatMap(entry -> {
            short elementTypeId = entry.getValue().getFirst().elementTypeId();
            ProtocolRecord protocol = protocolCache.computeIfAbsent(
                elementTypeId,
                id -> protocolRepository.getByProtocolAndElementTypeId(collector.protocol(), id)
            );
            return workerBuilder.apply(entry.getValue(), protocol).stream();
          })
          .toList();

      return workersService.get(workers);
    };
  }

  private List<Callable<List<MetricRecord>>> createSshWorkers(
      List<SourceRecord> sources, ProtocolRecord protocol) {
    return List.of(new SshWorker(sources, protocol));
  }

  private List<Callable<List<MetricRecord>>> createSnmpWorkers(
      List<SourceRecord> sources, ProtocolRecord protocol) {
    int maxOid = protocol.config().get("maxOid").asInt();
    return partition(sources, maxOid).stream()
        .map(chunk -> (Callable<List<MetricRecord>>) new SnmpWorker(chunk, protocol))
        .toList();
  }

  private static <T> List<List<T>> partition(List<T> list, int size) {
    List<List<T>> partitions = new ArrayList<>();
    for (int i = 0; i < list.size(); i += size) {
      partitions.add(list.subList(i, Math.min(i + size, list.size())));
    }
    return partitions;
  }
}