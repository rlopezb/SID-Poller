package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.aggregator.Aggregator;
import es.vodafone.sid.poller.finder.Finder;
import es.vodafone.sid.poller.model.Collector;
import es.vodafone.sid.poller.model.Discoverer;
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

  private final AggregatorFactory aggregatorFactory;
  private final FinderFactory finderFactory;
  private final MetricRepository metricRepository;
  private final CollectorRepository collectorRepository;
  private final DiscovererRepository discovererRepository;
  private final SourceRepository sourceRepository;

  private List<AggregatorService> aggregatorServices = List.of();
  private List<FinderService> finderServices = List.of();
  private final List<WorkerService> workerServices = new ArrayList<>();
  private final List<WalkerService> walkerServices = new ArrayList<>();

  @Override
  public void configureTasks(@NonNull ScheduledTaskRegistrar registrar) {
    // Initialize aggregator services
    // Reads all collector records from the database and creates a AggregatorService for each one
    // Schedules the aggregate method of each AggregatorService to run according to its cron expression
    this.aggregatorServices = collectorRepository.findAll().stream()
        .map(this::createAggregatorService)
        .toList();
    aggregatorServices.forEach(aggregatorService ->
        registrar.addCronTask(aggregatorService::aggregate, aggregatorService.getCollector().cron())
    );

    // Initialize finder services
    // Reads all discoverer records from the database and creates a FinderService for each one
    // Schedules the find method of each FinderService to run according to its cron expression
    this.finderServices = discovererRepository.findAll().stream()
        .map(this::createFinderService)
        .toList();
    finderServices.forEach(finderService ->
        registrar.addCronTask(finderService::find, finderService.getCron())
    );
  }

  private AggregatorService createAggregatorService(Collector collector) {
    // Create a new WorkerService for the collector and add it to the list of WorkerServices
    WorkerService workerService = new WorkerService(collector.workerTimeout(), collector.name());
    workerServices.add(workerService);
    // Create a new Aggregator for the AggregatorService using the aggregatorFactory and the collector
    Aggregator aggregator = aggregatorFactory.create(collector, workerService);
    return new AggregatorService(aggregator, collector, metricRepository);
  }

  private FinderService createFinderService(Discoverer discoverer) {
    // Create a new WalkersService for the discoverer and add it to the list of walkersServices
    WalkerService walkerService = new WalkerService(discoverer.workerTimeout(), discoverer.name());
    walkerServices.add(walkerService);
    // Create a new Finder for the FinderService using the finderFactory and the discoverer
    Finder finder = finderFactory.create(discoverer, walkerService);
    return new FinderService(finder, discoverer, sourceRepository);
  }

  @PreDestroy
  public void shutdown() {
    aggregatorServices.forEach(AggregatorService::shutdown);
    workerServices.forEach(WorkerService::shutdown);
    finderServices.forEach(FinderService::shutdown);
    walkerServices.forEach(WalkerService::shutdown);
  }
}