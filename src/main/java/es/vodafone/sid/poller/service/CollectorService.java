package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.SidData;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class CollectorService {

  private final ExecutorService executor;
  private final Callable<List<SidData>> collector;
  private final long collectorTimeout;

  public CollectorService(Callable<List<SidData>> collector, long collectorTimeout) {
    this.collector = collector;
    this.collectorTimeout = collectorTimeout;
    this.executor = Executors.newSingleThreadExecutor();
  }

  public void collect() {
    Future<List<SidData>> future = executor.submit(collector);
    try {
      List<SidData> result = future.get(collectorTimeout, TimeUnit.MILLISECONDS);
      log.debug("{} collect results with size: {}", Thread.currentThread().getName(), result.size());
      // TODO: persistir/publicar result
    } catch (InterruptedException e) {
      future.cancel(true);
      log.error("{} collect interrupted", Thread.currentThread().getName());
      Thread.currentThread().interrupt();
    } catch (ExecutionException | TimeoutException e) {
      future.cancel(true);
      log.error("{} collect failed ({})", Thread.currentThread().getName(), e.getClass().getSimpleName());
    }
  }

  @PreDestroy
  public void shutdown() {
    log.info("Shutting down {} CollectorService executor", Thread.currentThread().getName());
    executor.shutdown();
    try {
      if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
        log.warn("{} executor did not terminate, forcing shutdown", Thread.currentThread().getName());
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}