package es.vodafone.sid.poller.collector;

import es.vodafone.sid.poller.service.WorkersService;
import es.vodafone.sid.poller.worker.SshWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SshCollector extends Collector<SshWorker> {
  public SshCollector(WorkersService<SshWorker> workersService) {
    super(workersService);
  }

  @Override
  protected List<SshWorker> createWorkers() {
    return List.of(new SshWorker(), new SshWorker());
  }
}