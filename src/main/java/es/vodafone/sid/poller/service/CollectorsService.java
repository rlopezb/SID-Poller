package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.SidData;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class CollectorsService {
  private final ExecutorService executor;
  @Getter
  private final String name;
  @Getter
  private final String cron;
  private final Callable<List<SidData>> collector;
  private final long collectorTimeout;

  public CollectorsService(Callable<List<SidData>> collector, long collectorTimeout, String name, String cron) {
    this.name = name;
    this.collector = collector;
    this.collectorTimeout = collectorTimeout;
    this.cron = cron;
    this.executor = Executors.newSingleThreadExecutor();
  }

  public void collect() {
    Future<List<SidData>> future = executor.submit(collector);
    try {
      List<SidData> result = future.get(collectorTimeout, TimeUnit.MILLISECONDS);
      log.debug("{} collector results with size: {}", name, result.size());
      // TODO: persistir/publicar result
    } catch (InterruptedException e) {
      future.cancel(true);
      log.error("{} collector interrupted", name);
      Thread.currentThread().interrupt();
    } catch (ExecutionException | TimeoutException e) {
      future.cancel(true);
      log.error("{} collector failed ({})", name, e.getClass().getSimpleName());
    }
  }

  @PreDestroy
  public void shutdown() {
    log.info("Shutting down {} CollectorsService executor", name);
    executor.shutdown();
    try {
      if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
        log.warn("{} executor did not terminate, forcing shutdown", name);
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}