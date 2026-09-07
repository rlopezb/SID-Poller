package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.Source;
import es.vodafone.sid.poller.walker.Walker;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// This service is responsible for executing a list of Walker tasks concurrently and collecting their discovered sources.
@Slf4j
public class WalkerService {
  private final String name;
  private final long walkerTimeout;

  public WalkerService(long walkerTimeout, String name) {
    this.walkerTimeout = walkerTimeout;
    this.name = name;
  }

  // This method creates a custom thread factory that generates virtual threads
  // and sets an uncaught exception handler for logging errors
  private static ThreadFactory createThreadFactory(String name) {
    return new ThreadFactory() {
      private final AtomicInteger count = new AtomicInteger(0);

      @Override
      public Thread newThread(@NonNull Runnable runnable) {
        Thread thread = Thread.ofVirtual().name(name + "-walker-" + count.incrementAndGet()).unstarted(runnable);
        thread.setUncaughtExceptionHandler((t, e) ->
            log.error("Uncaught exception in thread {}: {}", t.getName(), e.getMessage(), e)
        );
        return thread;
      }
    };
  }

  // This method executes a list of walkers concurrently, collects their discovered sources,
  // and handles any exceptions or timeouts that may occur during execution
  public List<Source> run(List<Walker> walkers) {
    List<Source> discovered = new ArrayList<>();
    try (ExecutorService executor = Executors.newThreadPerTaskExecutor(createThreadFactory(name))) {
      List<Future<List<Source>>> futures = executor.invokeAll(walkers, walkerTimeout, TimeUnit.MILLISECONDS);
      for (Future<List<Source>> future : futures) {
        if (future.isCancelled()) {
          log.info("{} walker was cancelled", name);
        } else {
          try {
            List<Source> sources = future.get(walkerTimeout, TimeUnit.MILLISECONDS);
            if (sources != null) discovered.addAll(sources);
          } catch (InterruptedException e) {
            future.cancel(true);
            log.error("{} walker interrupted", name);
            Thread.currentThread().interrupt();
          } catch (ExecutionException e) {
            future.cancel(true);
            log.error("{} walker failed", name, e.getCause());
          } catch (TimeoutException e) {
            future.cancel(true);
            log.error("{} walker timeout after {} ms", name, walkerTimeout);
          }
        }
      }
    } catch (InterruptedException e) {
      log.error("{} executor interrupted", name);
      Thread.currentThread().interrupt();
    }
    return discovered;
  }
}