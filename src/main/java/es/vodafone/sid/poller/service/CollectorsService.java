package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.repository.MetricRepository;
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
  private final Callable<List<Metric>> collector;
  private final long collectorTimeout;
  private final MetricRepository metricRepository;

  public CollectorsService(Callable<List<Metric>> collector, long collectorTimeout, String name, String cron, MetricRepository metricRepository) {
    this.name = name;
    this.collector = collector;
    this.collectorTimeout = collectorTimeout;
    this.cron = cron;
    this.executor = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "CollectorsService-" + name);
      t.setDaemon(false);
      return t;
    });
    this.metricRepository = metricRepository;
  }

  public void collect() {
    Future<List<Metric>> future = executor.submit(collector);
    try {
      List<Metric> metrics = future.get(collectorTimeout, TimeUnit.MILLISECONDS);
      if (metrics != null) {
        log.debug("{} collector metrics with size: {}", name, metrics.size());
      } else {
        log.warn("{} collector returned null", name);
      }
      metricRepository.insert(metrics);
    } catch (InterruptedException e) {
      future.cancel(true);
      log.error("{} collector interrupted", name, e);
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