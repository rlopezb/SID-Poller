package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.CollectorRecord;
import es.vodafone.sid.poller.model.DiscovererRecord;
import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.model.SourceRecord;
import es.vodafone.sid.poller.repository.CollectorRepository;
import es.vodafone.sid.poller.repository.DiscovererRepository;
import es.vodafone.sid.poller.repository.MetricRepository;
import es.vodafone.sid.poller.repository.SourceRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService implements SchedulingConfigurer {

  private final CollectorFactory collectorFactory;
  private final DiscovererFactory discovererFactory;
  private final MetricRepository metricRepository;
  private final CollectorRepository collectorRepository;
  private final DiscovererRepository discovererRepository;
  private final SourceRepository sourceRepository;

  private List<CollectorService> collectorServices = List.of();
  private List<DiscovererService> discovererServices = List.of();
  private final List<WorkersService> workersServices = new ArrayList<>();
  private final List<WalkersService> walkersServices = new ArrayList<>();

  @Override
  public void configureTasks(@NonNull ScheduledTaskRegistrar registrar) {
    this.collectorServices = collectorRepository.findAll().stream()
        .map(this::createCollectorService)
        .toList();
    collectorServices.forEach(cs ->
        registrar.addCronTask(cs::collect, cs.getCollectorRecord().cron())
    );

    this.discovererServices = discovererRepository.findAll().stream()
        .map(this::createDiscovererService)
        .toList();
    discovererServices.forEach(ds ->
        registrar.addCronTask(ds::discover, ds.getCron())
    );
  }

  private CollectorService createCollectorService(CollectorRecord collectorRecord) {
    WorkersService workersService = new WorkersService(collectorRecord.workerTimeout(), collectorRecord.name());
    workersServices.add(workersService);
    Callable<List<MetricRecord>> collector = collectorFactory.create(collectorRecord, workersService);
    return new CollectorService(collector, collectorRecord, metricRepository);
  }

  private DiscovererService createDiscovererService(DiscovererRecord discovererRecord) {
    WalkersService walkerService = new WalkersService(discovererRecord.workerTimeout(), discovererRecord.name());
    walkersServices.add(walkerService);
    Callable<List<SourceRecord>> discoverer = discovererFactory.create(discovererRecord, walkerService);
    return new DiscovererService(discoverer, discovererRecord, sourceRepository);
  }

  @PreDestroy
  public void shutdown() {
    collectorServices.forEach(CollectorService::shutdown);
    workersServices.forEach(WorkersService::shutdown);
    discovererServices.forEach(DiscovererService::shutdown);
    walkersServices.forEach(WalkersService::shutdown);
  }
}