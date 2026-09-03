package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.aggregator.Aggregator;
import es.vodafone.sid.poller.finder.Finder;
import es.vodafone.sid.poller.model.Collector;
import es.vodafone.sid.poller.model.Discoverer;
import es.vodafone.sid.poller.repository.CollectorRepository;
import es.vodafone.sid.poller.repository.DiscovererRepository;
import es.vodafone.sid.poller.repository.MetricRepository;
import es.vodafone.sid.poller.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@RequiredArgsConstructor
// This service is responsible for scheduling the execution of collectors and discoverers based on their cron expressions.
public class SchedulerService implements SchedulingConfigurer {

  private final CollectorRepository collectorRepository;
  private final AggregatorFactory aggregatorFactory;
  private final MetricRepository metricRepository;

  private final DiscovererRepository discovererRepository;
  private final FinderFactory finderFactory;
  private final SourceRepository sourceRepository;

  @Override
  public void configureTasks(@NonNull ScheduledTaskRegistrar registrar) {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(5);
    scheduler.setThreadNamePrefix("PollerScheduler-");
    scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    scheduler.initialize();
    registrar.setTaskScheduler(scheduler);

    for (Collector collector : collectorRepository.findAll()) {
      WorkerService workerService = new WorkerService(collector.workerTimeout(), collector.name());
      Aggregator aggregator = aggregatorFactory.create(collector, workerService);
      AggregatorService aggregatorService = new AggregatorService(aggregator, collector, metricRepository);
      registrar.addCronTask(aggregatorService::aggregate, collector.cron());
    }

    for (Discoverer discoverer : discovererRepository.findAll()) {
      WalkerService walkerService = new WalkerService(discoverer.workerTimeout(), discoverer.name());
      Finder finder = finderFactory.create(discoverer, walkerService);
      FinderService finderService = new FinderService(finder, discoverer, sourceRepository);
      registrar.addCronTask(finderService::find, discoverer.cron());
    }
  }
}