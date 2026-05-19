package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.collector.SshCollector;
import es.vodafone.sid.poller.config.SshCollectorConfiguration;
import es.vodafone.sid.poller.model.SidData;
import jakarta.annotation.PreDestroy;
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

  public void collect() {
    Future<List<SidData>> future = executor.submit(sshCollector);
    List<SidData> result = new ArrayList<>();
    try {
      result.addAll(future.get(sshCollectorConfiguration.collectorTimeout(), TimeUnit.MILLISECONDS));
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      future.cancel(true);
      log.error("SshCollectorService collect failed ({})", e.getClass().getSimpleName());
    }
    log.debug("SshCollectorService collect results with size: {}", result.size());
  }

  @PreDestroy
  public void shutdown() {
    log.info("Shutting down SshCollectorService executor");
    executor.shutdown();
    try {
      if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
        log.warn("Executor did not terminate, forcing shutdown");
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
