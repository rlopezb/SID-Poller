package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.SidData;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class WorkersService {
  private final ExecutorService executor;
  private final long workerTimeout;

  public WorkersService(long workerTimeout) {
    this.executor = Executors.newVirtualThreadPerTaskExecutor();
    this.workerTimeout = workerTimeout;
  }

  public List<SidData> get(List<Callable<List<SidData>>> workers) {
    List<Future<List<SidData>>> futures = workers.stream()
        .map(executor::submit)
        .toList();

    List<SidData> results = new ArrayList<>();
    for (Future<List<SidData>> future : futures) {
      try {
        results.addAll(future.get(workerTimeout, TimeUnit.MILLISECONDS));
      } catch (InterruptedException e) {
        future.cancel(true);
        log.error("{} worker interrupted", Thread.currentThread().getName());
        Thread.currentThread().interrupt();
      } catch (ExecutionException | TimeoutException e) {
        future.cancel(true);
        log.error("{} worker failed ({})", Thread.currentThread().getName(), e.getClass().getSimpleName());
      }
    }
    return results;
  }

  @PreDestroy
  public void shutdown() {
    log.info("Shutting down {} WorkersService executor", Thread.currentThread().getName());
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