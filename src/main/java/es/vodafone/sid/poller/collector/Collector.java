package es.vodafone.sid.poller.collector;

import es.vodafone.sid.poller.model.SidData;
import es.vodafone.sid.poller.service.WorkersService;

import java.util.List;
import java.util.concurrent.Callable;

public abstract class Collector<W extends Callable<List<SidData>>>
    implements Callable<List<SidData>> {

  private final WorkersService<W> workersService;

  protected Collector(WorkersService<W> workersService) {
    this.workersService = workersService;
  }

  protected abstract List<W> createWorkers();

  @Override
  public List<SidData> call() {
    return workersService.get(createWorkers());
  }
}