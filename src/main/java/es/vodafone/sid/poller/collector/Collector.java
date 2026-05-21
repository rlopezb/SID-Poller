package es.vodafone.sid.poller.collector;

import es.vodafone.sid.poller.model.SidData;
import es.vodafone.sid.poller.service.WorkersService;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class Collector implements Callable<List<SidData>> {

  private final WorkersService workersService;
  private final Supplier<List<Callable<List<SidData>>>> workersSupplier;

  public Collector(WorkersService workersService, Supplier<List<Callable<List<SidData>>>> workersSupplier) {
    this.workersService = workersService;
    this.workersSupplier = workersSupplier;
  }

  @Override
  public List<SidData> call() {
    return workersService.get(workersSupplier.get());
  }
}