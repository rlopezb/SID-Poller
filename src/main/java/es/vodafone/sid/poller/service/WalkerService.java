package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.Source;
import es.vodafone.sid.poller.walker.Walker;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
// This service is responsible for executing a list of Walker tasks concurrently with a specified timeout.
public class WalkerService {
  private final ExecutorService executor;
  private final String name;
  private final long walkerTimeout;

  public WalkerService(long walkerTimeout, String name) {
    this.walkerTimeout = walkerTimeout;
    this.name = name;
    this.executor = Executors.newThreadPerTaskExecutor(createThreadFactory(name));
  }

  private static ThreadFactory createThreadFactory(String poolName) {
    return new ThreadFactory() {
      private final AtomicInteger count = new AtomicInteger(0);

      @Override
      public Thread newThread(@NonNull Runnable runnable) {
        Thread thread = Thread.ofVirtual()
            .name(poolName + "-walker-" + count.incrementAndGet())
            .unstarted(runnable);
        thread.setUncaughtExceptionHandler((t, e) ->
            log.error("Uncaught exception in thread {}: {}", t.getName(), e.getMessage(), e)
        );
        return thread;
      }
    };
  }

  public List<Source> get(List<Walker> walkers) {
    List<Source> discovered = new ArrayList<>();
    try {
      List<Future<List<Source>>> futures = executor.invokeAll(walkers, walkerTimeout, TimeUnit.MILLISECONDS);
      for (Future<List<Source>> future : futures) {
        if (future.isCancelled()) {
          log.info("{} walker was cancelled", name);
        } else {
          try {
            List<Source> sources = future.get();
            if (sources != null) discovered.addAll(sources);
          } catch (ExecutionException e) {
            log.error("{} walker failed", name, e.getCause());
          }
        }
      }
    } catch (InterruptedException e) {
      log.error("{} walker executor interrupted", name);
      Thread.currentThread().interrupt();
    }
    return discovered;
  }

  public void shutdown() {
    log.info("Shutting down {} WalkerService executor", name);
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