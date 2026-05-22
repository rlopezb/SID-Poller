package es.vodafone.sid.poller.collector;

import es.vodafone.sid.poller.model.SidData;
import es.vodafone.sid.poller.service.WorkersService;
import es.vodafone.sid.poller.worker.SnmpWorker;
import es.vodafone.sid.poller.worker.SshWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;


public class CollectorFactory {
  public static Collector create(String protocol, WorkersService workersService) {
    WorkersSupplier supplier = switch (protocol.toUpperCase()) {
      case "SNMP" -> () -> createWorkers(SnmpWorker::new, 50);
      case "SSH"  -> () -> createWorkers(SshWorker::new, 100);
      default -> throw new IllegalArgumentException("Unknown protocol: " + protocol);
    };
    return new Collector(workersService, supplier);
  }

  private static List<Callable<List<SidData>>> createWorkers(
      Supplier<? extends Callable<List<SidData>>> workerFactory, int count) {
    List<Callable<List<SidData>>> workers = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      workers.add(workerFactory.get());
    }
    return workers;
  }
}