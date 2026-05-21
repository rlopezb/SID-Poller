package es.vodafone.sid.poller.collector;

import es.vodafone.sid.poller.service.WorkersService;

import java.util.List;
import java.util.function.Supplier;

public class CollectorFactory {

  public static Collector create(String protocol, WorkersService workersService, Supplier<List<?>> workersSupplier) {
    return switch (protocol.toUpperCase()) {
      case "SNMP" -> new Collector(workersService, workersSupplier);
      case "SSH"  -> new Collector(workersService, workersSupplier);
      default -> throw new IllegalArgumentException("Unknown protocol: " + protocol);
    };
  }
}