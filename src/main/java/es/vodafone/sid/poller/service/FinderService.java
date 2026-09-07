package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.finder.Finder;
import es.vodafone.sid.poller.model.Discoverer;
import es.vodafone.sid.poller.model.Source;
import es.vodafone.sid.poller.repository.SourceRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class FinderService {

  @Getter
  private final Finder finder;
  private final Discoverer discoverer;
  private final SourceRepository sourceRepository;

  public FinderService(Finder finder,
                       Discoverer discoverer,
                       SourceRepository sourceRepository) {
    this.finder = finder;
    this.discoverer = discoverer;
    this.sourceRepository = sourceRepository;
  }

  public void find() {
    Future<List<Source>> future = null;
    ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "FinderService-" + discoverer.name());
      thread.setDaemon(true);
      return thread;
    });
    try {
      future = executor.submit(finder);
      List<Source> found = future.get(discoverer.discovererTimeout(), TimeUnit.MILLISECONDS);
      if (found != null) {
        log.debug("{} finder found {} sources", discoverer.name(), found.size());
        Map<String, List<Source>> discoveredByKey = found.stream()
            .collect(Collectors.groupingBy(s -> s.elementId() + ":" + s.collectorId()));

        for (Map.Entry<String, List<Source>> entry : discoveredByKey.entrySet()) {
          String[] parts = entry.getKey().split(":");
          short elementId = Short.parseShort(parts[0]);
          short collectorId = Short.parseShort(parts[1]);

          List<Source> discoveredGroup = entry.getValue();
          List<Source> existingGroup = sourceRepository.findByElementIdAndCollectorIdAndDiscovererId(elementId, collectorId, discoverer.id());

          List<Source> toInsert = discoveredGroup.stream()
              .filter(candidate -> existingGroup.stream().noneMatch(candidate::isSame))
              .toList();
          if (!toInsert.isEmpty()) {
            log.info("{} inserting {} new sources for element {}",
                discoverer.name(), toInsert.size(), elementId);
            toInsert.forEach(sourceRepository::insert);
          }

          List<Source> toReactivate = existingGroup.stream()
              .filter(existing -> discoveredGroup.stream().anyMatch(existing::isSame))
              .toList();
          toReactivate.forEach(source -> sourceRepository.setActive(source.id(), true));

          List<Source> disappeared = existingGroup.stream()
              .filter(existing -> discoveredGroup.stream().noneMatch(existing::isSame))
              .toList();
          List<Source> toDeactivate = disappeared.stream()
              .filter(source -> sourceRepository.hasMetrics(source.id()))
              .toList();
          if (!toDeactivate.isEmpty()) {
            log.info("{} deactivating {} disappeared sources with metrics for element {}",
                discoverer.name(), toDeactivate.size(), elementId);
            toDeactivate.forEach(source -> sourceRepository.setActive(source.id(), false));
          }

          List<Source> toDelete = disappeared.stream()
              .filter(source -> !toDeactivate.contains(source))
              .toList();
          if (!toDelete.isEmpty()) {
            log.info("{} deleting {} disappeared sources without metrics for element {}",
                discoverer.name(), toDelete.size(), elementId);
            toDelete.forEach(source -> sourceRepository.deleteById(source.id()));
          }
        }
      } else {
        log.warn("{} finder returned null", discoverer.name());
      }
    } catch (InterruptedException e) {
      future.cancel(true);
      log.error("{} finder interrupted", discoverer.name(), e);
      Thread.currentThread().interrupt();
    } catch (ExecutionException | TimeoutException e) {
      future.cancel(true);
      log.error("{} finder failed ({})", discoverer.name(), e.getClass().getSimpleName());
    } finally {
      executor.shutdownNow();
    }
  }
}