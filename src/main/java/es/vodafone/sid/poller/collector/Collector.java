package es.vodafone.sid.poller.collector;

import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.service.WorkersService;

import java.util.List;
import java.util.concurrent.Callable;

public class Collector implements Callable<List<Metric>> {

  private final WorkersService workersService;
  private final WorkersSupplier workersSupplier;

  public Collector(WorkersService workersService, WorkersSupplier workersSupplier) {
    this.workersService = workersService;
    this.workersSupplier = workersSupplier;
  }

  @Override
  public List<Metric> call() {
    return workersService.get(workersSupplier.get());
  }
}