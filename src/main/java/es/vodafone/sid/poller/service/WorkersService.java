package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.SidData;
import jakarta.annotation.PreDestroy;
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

  public List<SidData> get(List<Callable<List<SidData>>> workers) {
    List<SidData> results = new ArrayList<>();
    workers.stream()
        .map(executor::submit)
        .forEach(future -> {
          try {
            List<SidData> data = future.get(workerTimeout, TimeUnit.MILLISECONDS);
            if (data != null) results.addAll(data);
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
        });
    return results;
  }

  @PreDestroy
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