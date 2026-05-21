package es.vodafone.sid.poller.collector;

import es.vodafone.sid.poller.service.WorkersService;
import es.vodafone.sid.poller.worker.SnmpWorker;
import es.vodafone.sid.poller.worker.SshWorker;
import es.vodafone.sid.poller.worker.WorkersSupplier;

import java.util.List;


public class CollectorFactory {
  public static Collector create(String protocol, WorkersService workersService) {
    WorkersSupplier supplier = switch (protocol.toUpperCase()) {
      case "SNMP" -> () -> List.of(new SnmpWorker());
      case "SSH"  -> () -> List.of(new SshWorker());
      default -> throw new IllegalArgumentException("Unknown protocol: " + protocol);
    };
    return new Collector(workersService, supplier);
  }
}