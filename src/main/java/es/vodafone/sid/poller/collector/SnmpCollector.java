package es.vodafone.sid.poller.collector;

import es.vodafone.sid.poller.service.WorkersService;
import es.vodafone.sid.poller.worker.SnmpWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SnmpCollector extends Collector<SnmpWorker> {
  public SnmpCollector(WorkersService<SnmpWorker> workersService) {
    super(workersService);
  }

  @Override
  protected List<SnmpWorker> createWorkers() {
    return List.of(new SnmpWorker(), new SnmpWorker(), new SnmpWorker());
  }
}