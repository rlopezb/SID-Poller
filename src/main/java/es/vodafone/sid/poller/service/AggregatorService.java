package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.aggregator.Aggregator;
import es.vodafone.sid.poller.model.Collector;
import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.repository.MetricRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class AggregatorService {
  @Getter
  private final Collector collector;
  private final Aggregator aggregator;
  private final MetricRepository metricRepository;

  private final AtomicReference<ExecutorService> executorRef = new AtomicReference<>();
  private final AtomicReference<Future<List<Metric>>> activeFuture = new AtomicReference<>();

  public AggregatorService(Aggregator aggregator, Collector collector, MetricRepository metricRepository) {
    this.aggregator = aggregator;
    this.collector = collector;
    this.metricRepository = metricRepository;
    this.executorRef.set(newExecutor());
  }

  private ExecutorService newExecutor() {
    return Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "AggregatorService-" + collector.name());
      thread.setDaemon(true); // si queda huérfano por un cuelgue real, no bloquea el shutdown de la JVM
      return thread;
    });
  }

  public void aggregate() {
    Future<List<Metric>> previous = activeFuture.get();
    if (previous != null && !previous.isDone()) {
      log.warn("{} sigue en ejecución, se descarta y se sustituye el executor", collector.name());
      previous.cancel(true);
      // El hilo puede seguir vivo si ignora la interrupción (DNS, cuelgue de librería).
      // Lo abandonamos: nuevo executor, el viejo se descarta sin esperar a que termine.
      ExecutorService old = executorRef.getAndSet(newExecutor());
      old.shutdownNow();
    }

    ExecutorService executor = executorRef.get();
    Future<List<Metric>> future = executor.submit(aggregator);
    activeFuture.set(future);

    try {
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

  public void shutdown() {
    log.info("Shutting down {} AggregatorService executor", collector.name());
    ExecutorService executor = executorRef.get();
    executor.shutdown();
    try {
      if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}