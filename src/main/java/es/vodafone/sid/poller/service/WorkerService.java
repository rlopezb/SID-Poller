package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.strategy.BaseSourceType;
import es.vodafone.sid.poller.worker.Worker;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// This service is responsible for executing a list of workers concurrently and collecting their results.
@Slf4j
public class WorkerService {
  private final String name;
  private final long workerTimeout;

  public WorkerService(long workerTimeout, String name) {
    this.workerTimeout = workerTimeout;
    this.name = name;
  }

  // This method creates a custom thread factory that generates virtual threads
  // and sets an uncaught exception handler for logging errors
  private static ThreadFactory createThreadFactory(String name) {
    return new ThreadFactory() {
      private final AtomicInteger count = new AtomicInteger(0);

      @Override
      public Thread newThread(@NonNull Runnable runnable) {
        Thread thread = Thread.ofVirtual().name(name + "-worker-" + count.incrementAndGet()).unstarted(runnable);
        thread.setUncaughtExceptionHandler((t, e) ->
            log.error("Uncaught exception in thread {}: {}", t.getName(), e.getMessage(), e)
        );
        return thread;
      }
    };
  }

  // This method executes a list of workers concurrently, collects their metrics,
  // and handles any exceptions or timeouts that may occur during execution
  public List<Metric> get(List<Worker> workers) {
    List<Future<List<Metric>>> futures = null;
    List<Metric> workersMetrics = new ArrayList<>();
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    try (ExecutorService executor = Executors.newThreadPerTaskExecutor(createThreadFactory(name))) {
      futures = executor.invokeAll(workers, workerTimeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      log.error("{} executor interrupted", name);
      Thread.currentThread().interrupt();
    }
    if (futures != null) {
      for (int i = 0; i < futures.size(); i++) {
        Future<List<Metric>> future = futures.get(i);
        Worker worker = workers.get(i);
        if (future.isCancelled()) {
          log.info("{} worker was cancelled", name);
          workersMetrics.addAll(nullMetrics(worker, now));
        } else {
          try {
            List<Metric> workerMetrics = future.get(workerTimeout, TimeUnit.MILLISECONDS);
            workersMetrics.addAll(Objects.requireNonNullElseGet(workerMetrics, () -> nullMetrics(worker, now)));
          } catch (InterruptedException e) {
            future.cancel(true);
            log.error("{} worker interrupted", name);
            workersMetrics.addAll(nullMetrics(worker, now));
            Thread.currentThread().interrupt();
          } catch (ExecutionException e) {
            future.cancel(true);
            log.error("{} worker failed", name, e.getCause());
            workersMetrics.addAll(nullMetrics(worker, now));
          } catch (TimeoutException e) {
            future.cancel(true);
            log.error("{} worker timeout after {} ms", name, workerTimeout);
            workersMetrics.addAll(nullMetrics(worker, now));
          }
        }
      }
    } else {
      log.warn("{} no workers were executed", name);
      for (Worker worker : workers) {
        workersMetrics.addAll(nullMetrics(worker, now));
      }
    }
    return workersMetrics;
  }

  // This method generates a list of null metrics for a given worker and instant
  private List<Metric> nullMetrics(Worker worker, OffsetDateTime instant) {
    return worker.getSources().stream()
        .map(source -> BaseSourceType.nullMetric(source, instant))
        .toList();
  }
}