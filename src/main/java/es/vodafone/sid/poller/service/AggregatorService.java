package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.aggregator.Aggregator;
import es.vodafone.sid.poller.model.Collector;
import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.repository.MetricRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class AggregatorService {
  @Getter
  private final Collector collector;
  private final Aggregator aggregator;
  private final MetricRepository metricRepository;

  public AggregatorService(Aggregator aggregator, Collector collector, MetricRepository metricRepository) {
    this.aggregator = aggregator;
    this.collector = collector;
    this.metricRepository = metricRepository;
  }

  public void aggregate() {
    Future<List<Metric>> future = null;
    try (ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "AggregatorService-" + collector.name());
      thread.setDaemon(true);
      return thread;
    })) {
      future = executor.submit(aggregator);
      List<Metric> metrics = future.get(collector.collectorTimeout(), TimeUnit.MILLISECONDS);
      if (metrics != null) {
        log.debug("{} aggregator metrics con tamaño: {}", collector.name(), metrics.size());
        metricRepository.insert(metrics);
      } else {
        log.warn("{} aggregator devolvió null", collector.name());
      }
    } catch (InterruptedException e) {
      future.cancel(true);
      Thread.currentThread().interrupt();
    } catch (ExecutionException | TimeoutException e) {
      future.cancel(true);
      log.error("{} aggregator falló o superó el timeout de esta ejecución", collector.name(), e);
    }
  }
}