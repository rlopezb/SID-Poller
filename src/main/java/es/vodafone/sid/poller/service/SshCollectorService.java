package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.collector.SshCollector;
import es.vodafone.sid.poller.config.SshCollectorConfiguration;
import es.vodafone.sid.poller.model.SidData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
@Slf4j
class SshCollectorService {
  private final ExecutorService executor;
  private final SshCollector sshCollector;
  private final SshCollectorConfiguration sshCollectorConfiguration;

  public SshCollectorService(SshCollector sshCollector, SshCollectorConfiguration sshCollectorConfiguration) {
    this.sshCollector = sshCollector;
    this.sshCollectorConfiguration = sshCollectorConfiguration;
    this.executor = Executors.newSingleThreadExecutor();
  }

  public List<SidData> collect() {
    Future<List<SidData>> future = executor.submit(sshCollector);
    List<SidData> result = null;
    try {
      result = new ArrayList<>(future.get(sshCollectorConfiguration.collectorTimeout(), TimeUnit.MILLISECONDS));
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      future.cancel(true);
      log.error("SshCollectorService collect failed ({})", e.getClass().getSimpleName());
    }
    return result;
  }

  public void shutdown() {
    executor.shutdownNow();
  }
}
