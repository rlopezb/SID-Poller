package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.DiscovererRecord;
import es.vodafone.sid.poller.model.SourceRecord;
import es.vodafone.sid.poller.repository.SourceRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class DiscovererService {

  @Getter
  private final DiscovererRecord discovererRecord;
  private final Callable<List<SourceRecord>> discoverer;
  private final SourceRepository sourceRepository;
  private final ExecutorService executor;

  public DiscovererService(Callable<List<SourceRecord>> discoverer,
                           DiscovererRecord discovererRecord,
                           SourceRepository sourceRepository) {
    this.discoverer = discoverer;
    this.discovererRecord = discovererRecord;
    this.sourceRepository = sourceRepository;
    this.executor = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "DiscovererService-" + discovererRecord.name());
      t.setDaemon(false);
      return t;
    });
  }

  public void discover() {
    Future<List<SourceRecord>> future = executor.submit(discoverer);
    try {
      List<SourceRecord> discovered = future.get(discovererRecord.discovererTimeout(), TimeUnit.MILLISECONDS);
      if (discovered != null) {
        log.debug("{} discoverer found {} sources", discovererRecord.name(), discovered.size());
        reconcile(discovered);
      } else {
        log.warn("{} discoverer returned null", discovererRecord.name());
      }
    } catch (InterruptedException e) {
      future.cancel(true);
      log.error("{} discoverer interrupted", discovererRecord.name(), e);
      Thread.currentThread().interrupt();
    } catch (ExecutionException | TimeoutException e) {
      future.cancel(true);
      log.error("{} discoverer failed ({})", discovererRecord.name(), e.getClass().getSimpleName());
    }
  }

  private void reconcile(List<SourceRecord> discovered) {
    Map<String, List<SourceRecord>> discoveredByKey = discovered.stream()
        .collect(Collectors.groupingBy(s -> s.elementId() + ":" + s.collectorId()));

    for (Map.Entry<String, List<SourceRecord>> entry : discoveredByKey.entrySet()) {
      String[] parts = entry.getKey().split(":");
      short elementId = Short.parseShort(parts[0]);
      short collectorId = Short.parseShort(parts[1]);

      List<SourceRecord> discoveredGroup = entry.getValue();
      List<SourceRecord> existingGroup = sourceRepository
          .findByElementIdAndCollectorId(elementId, collectorId);

      List<SourceRecord> toInsert = discoveredGroup.stream()
          .filter(candidate -> existingGroup.stream().noneMatch(candidate::isSame))
          .toList();
      if (!toInsert.isEmpty()) {
        log.info("{} inserting {} new sources for element {}",
            discovererRecord.name(), toInsert.size(), elementId);
        toInsert.forEach(sourceRepository::insert);
      }

      List<SourceRecord> toDelete = existingGroup.stream()
          .filter(existing -> discoveredGroup.stream().noneMatch(existing::isSame))
          .toList();
      if (!toDelete.isEmpty()) {
        log.info("{} deleting {} disappeared sources for element {}",
            discovererRecord.name(), toDelete.size(), elementId);
        toDelete.forEach(s -> sourceRepository.deleteById(s.id()));
      }
    }
  }

  public String getCron() {
    return discovererRecord.cron();
  }

  public void shutdown() {
    log.info("Shutting down {} DiscovererService executor", discovererRecord.name());
    executor.shutdown();
    try {
      if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
        log.warn("{} executor did not terminate, forcing shutdown", discovererRecord.name());
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}