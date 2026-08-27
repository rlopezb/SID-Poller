package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.collector.Collector;
import es.vodafone.sid.poller.discoverer.Discoverer;
import es.vodafone.sid.poller.model.CollectorRecord;
import es.vodafone.sid.poller.model.DiscovererRecord;
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

@Slf4j
@Service
@RequiredArgsConstructor
// This service is responsible for scheduling the execution of collectors and discoverers based on their cron expressions.
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
    // Initialize collector services
    // Reads all collector records from the database and creates a CollectorService for each one
    // Schedules the collect method of each CollectorService to run according to its cron expression
    this.collectorServices = collectorRepository.findAll().stream()
        .map(this::createCollectorService)
        .toList();
    collectorServices.forEach(cs ->
        registrar.addCronTask(cs::collect, cs.getCollectorRecord().cron())
    );

    // Initialize discoverer services
    // Reads all discoverer records from the database and creates a DiscovererService for each one
    // Schedules the discover method of each DiscovererService to run according to its cron expression
    this.discovererServices = discovererRepository.findAll().stream()
        .map(this::createDiscovererService)
        .toList();
    discovererServices.forEach(ds ->
        registrar.addCronTask(ds::discover, ds.getCron())
    );
  }

  private CollectorService createCollectorService(CollectorRecord collectorRecord) {
    // Create a new WorkersService for the collector and add it to the list of workersServices
    WorkersService workersService = new WorkersService(collectorRecord.workerTimeout(), collectorRecord.name());
    workersServices.add(workersService);
    // Create a new Collector for the CollectorService using the collectorFactory and the collectorRecord
    Collector collector = collectorFactory.create(collectorRecord, workersService);
    return new CollectorService(collector, collectorRecord, metricRepository);
  }

  private DiscovererService createDiscovererService(DiscovererRecord discovererRecord) {
    // Create a new WalkersService for the discoverer and add it to the list of walkersServices
    WalkersService walkerService = new WalkersService(discovererRecord.workerTimeout(), discovererRecord.name());
    walkersServices.add(walkerService);
    // Create a new Discoverer for the DiscovererService using the discovererFactory and the discovererRecord
    Discoverer discoverer = discovererFactory.create(discovererRecord, walkerService);
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