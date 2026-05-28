package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.repository.ProtocolRepository;
import es.vodafone.sid.poller.repository.SourceRepository;
import es.vodafone.sid.poller.worker.SnmpWorker;
import es.vodafone.sid.poller.worker.SshWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class CollectorFactory {
  private final SourceRepository sourceRepository;
  private final ProtocolRepository protocolRepository;

  @FunctionalInterface
  private interface WorkersSupplier {
    List<Callable<List<MetricRecord>>> get();
  }

  public Callable<List<MetricRecord>> create(String protocol, WorkersService workersService) {
    WorkersSupplier workersSupplier = switch (protocol.toUpperCase()) {
      case "SNMP" -> () -> createWorkers(SnmpWorker::new, 50);
      case "SSH"  -> () -> createWorkers(SshWorker::new, 100);
      default -> throw new IllegalArgumentException("Unknown protocol: " + protocol);
    };
    return () -> workersService.get(workersSupplier.get());
  }

  private static List<Callable<List<MetricRecord>>> createWorkers(
      Supplier<? extends Callable<List<MetricRecord>>> workerFactory, int count) {
    List<Callable<List<MetricRecord>>> workers = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      workers.add(workerFactory.get());
    }
    return workers;
  }
}