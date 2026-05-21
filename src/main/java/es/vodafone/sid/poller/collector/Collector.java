package es.vodafone.sid.poller.collector;

import es.vodafone.sid.poller.model.SidData;
import es.vodafone.sid.poller.service.WorkersService;
import es.vodafone.sid.poller.worker.WorkersSupplier;

import java.util.List;
import java.util.concurrent.Callable;

public class Collector implements Callable<List<SidData>> {

  private final WorkersService workersService;
  private final WorkersSupplier workersSupplier;

  public Collector(WorkersService workersService, WorkersSupplier workersSupplier) {
    this.workersService = workersService;
    this.workersSupplier = workersSupplier;
  }

  @Override
  public List<SidData> call() {
    return workersService.get(workersSupplier.get());
  }
}