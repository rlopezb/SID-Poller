package es.vodafone.sid.poller.collector;

import es.vodafone.sid.poller.model.SidData;
import es.vodafone.sid.poller.service.SshWorkersService;
import es.vodafone.sid.poller.worker.SshWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@Component
@RequiredArgsConstructor
public class SshCollector implements Callable<List<SidData>> {
  private final SshWorkersService sshWorkersService;

  @Override
  public List<SidData> call() {
    List<SshWorker> workers = new ArrayList<>();
    workers.add(new SshWorker());
    workers.add(new SshWorker());
    return sshWorkersService.get(workers);
  }
}
