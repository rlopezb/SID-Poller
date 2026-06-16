package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.MetricRecord;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class WorkersService {
  private final ExecutorService executor;
  private final String name;
  private final long workerTimeout;

  public WorkersService(long workerTimeout, String name) {
    this.workerTimeout = workerTimeout;
    this.name = name;
    this.executor = Executors.newThreadPerTaskExecutor(createThreadFactory(name));
  }

  private static ThreadFactory createThreadFactory(String poolName) {
    return new ThreadFactory() {
      private final AtomicInteger count = new AtomicInteger(0);

      @Override
      public Thread newThread(@NonNull Runnable runnable) {
        Thread thread = Thread.ofVirtual()
            .name(poolName + "-worker-" + count.incrementAndGet())
            .unstarted(runnable);
        thread.setUncaughtExceptionHandler((t, e) ->
            log.error("Uncaught exception in thread {}: {}", t.getName(), e.getMessage(), e)
        );
        return thread;
      }
    };
  }

  public List<MetricRecord> get(List<Callable<List<MetricRecord>>> workers) {
    List<Future<List<MetricRecord>>> futures = null;
    try {
      futures = executor.invokeAll(workers, workerTimeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      log.error("{} executor interrupted", name);
      Thread.currentThread().interrupt();
    }
    List<MetricRecord> workersMetricRecords = new ArrayList<>();
    if (futures != null) {
      for (Future<List<MetricRecord>> future : futures) {
        if (future.isCancelled()) {
          log.info("{} worker was cancelled", name);
        } else {
          try {
            List<MetricRecord> workerMetricRecords = future.get(workerTimeout, TimeUnit.MILLISECONDS);
            if (workerMetricRecords != null) workersMetricRecords.addAll(workerMetricRecords);
          } catch (InterruptedException e) {
            future.cancel(true);
            log.error("{} worker interrupted", name);
            Thread.currentThread().interrupt();
          } catch (ExecutionException e) {
            future.cancel(true);
            log.error("{} worker failed", name, e.getCause());
          } catch (TimeoutException e) {
            future.cancel(true);
            log.info("{} worker timeout after {} ms", name, workerTimeout);
          }
        }
      }
    } else {
      log.warn("{} no workers were executed", name);
    }
    return workersMetricRecords;
  }

  public void shutdown() {
    log.info("Shutting down {} WorkersService executor", name);
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