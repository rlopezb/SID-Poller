package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.CollectorRecord;
import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.repository.MetricRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class CollectorService {
  private final ExecutorService executor;
  @Getter
  private final CollectorRecord collectorRecord;
  private final Callable<List<MetricRecord>> collector;
  private final MetricRepository metricRepository;

  public CollectorService(Callable<List<MetricRecord>> collector, CollectorRecord collectorRecord, MetricRepository metricRepository) {
    this.collectorRecord = collectorRecord;
    this.collector = collector;
    this.executor = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "CollectorsService-" + collectorRecord.name());
      t.setDaemon(false);
      return t;
    });
    this.metricRepository = metricRepository;
  }

  public void collect() {
    Future<List<MetricRecord>> future = executor.submit(collector);
    try {
      List<MetricRecord> metrics = future.get(collectorRecord.collectorTimeout(), TimeUnit.MILLISECONDS);
      if (metrics != null) {
        log.debug("{} collector metrics with size: {}", collectorRecord.name(), metrics.size());
        metricRepository.insert(metrics);
      } else {
        log.warn("{} collector returned null", collectorRecord.name());
      }
    } catch (InterruptedException e) {
      future.cancel(true);
      log.error("{} collector interrupted", collectorRecord.name(), e);
      Thread.currentThread().interrupt();
    } catch (ExecutionException | TimeoutException e) {
      future.cancel(true);
      log.error("{} collector failed", collectorRecord.name(), e);
    }
  }

  public void shutdown() {
    log.info("Shutting down {} CollectorsService executor", collectorRecord.name());
    executor.shutdown();
    try {
      if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
        log.warn("{} executor did not terminate, forcing shutdown", collectorRecord.name());
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}