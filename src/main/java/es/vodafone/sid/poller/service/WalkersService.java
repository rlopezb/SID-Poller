package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.SourceRecord;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class WalkersService {
  private final ExecutorService executor;
  private final String name;
  private final long walkerTimeout;

  public WalkersService(long walkerTimeout, String name) {
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

  public List<SourceRecord> get(List<Callable<List<SourceRecord>>> walkers) {
    List<SourceRecord> discovered = new ArrayList<>();
    try {
      List<Future<List<SourceRecord>>> futures = executor.invokeAll(walkers, walkerTimeout, TimeUnit.MILLISECONDS);
      for (Future<List<SourceRecord>> future : futures) {
        if (future.isCancelled()) {
          log.info("{} walker was cancelled", name);
        } else {
          try {
            List<SourceRecord> sources = future.get();
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