package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.collector.Collector;
import es.vodafone.sid.poller.model.CollectorRecord;
import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.repository.MetricRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class CollectorService {
  @Getter
  private final CollectorRecord collectorRecord;
  private final Collector collector;
  private final MetricRepository metricRepository;

  private final AtomicReference<ExecutorService> executorRef = new AtomicReference<>();
  private final AtomicReference<Future<List<MetricRecord>>> activeFuture = new AtomicReference<>();

  public CollectorService(Collector collector, CollectorRecord collectorRecord, MetricRepository metricRepository) {
    this.collectorRecord = collectorRecord;
    this.collector = collector;
    this.metricRepository = metricRepository;
    this.executorRef.set(newExecutor());
  }

  private ExecutorService newExecutor() {
    return Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "CollectorService-" + collectorRecord.name());
      thread.setDaemon(true); // si queda huérfano por un cuelgue real, no bloquea el shutdown de la JVM
      return thread;
    });
  }

  public void collect() {
    Future<List<MetricRecord>> previous = activeFuture.get();
    if (previous != null && !previous.isDone()) {
      log.warn("{} sigue en ejecución, se descarta y se sustituye el executor", collectorRecord.name());
      previous.cancel(true);
      // El hilo puede seguir vivo si ignora la interrupción (DNS, cuelgue de librería).
      // Lo abandonamos: nuevo executor, el viejo se descarta sin esperar a que termine.
      ExecutorService old = executorRef.getAndSet(newExecutor());
      old.shutdownNow();
    }

    ExecutorService executor = executorRef.get();
    Future<List<MetricRecord>> future = executor.submit(collector);
    activeFuture.set(future);

    try {
      List<MetricRecord> metrics = future.get(collectorRecord.collectorTimeout(), TimeUnit.MILLISECONDS);
      if (metrics != null) {
        log.debug("{} collector metrics con tamaño: {}", collectorRecord.name(), metrics.size());
        metricRepository.insert(metrics);
      } else {
        log.warn("{} collector devolvió null", collectorRecord.name());
      }
    } catch (InterruptedException e) {
      future.cancel(true);
      Thread.currentThread().interrupt();
    } catch (ExecutionException | TimeoutException e) {
      future.cancel(true);
      log.error("{} collector falló o superó el timeout de esta ejecución", collectorRecord.name(), e);
    }
  }

  public void shutdown() {
    log.info("Shutting down {} CollectorService executor", collectorRecord.name());
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