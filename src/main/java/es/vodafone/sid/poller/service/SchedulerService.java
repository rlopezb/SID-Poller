package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.collector.Collector;
import es.vodafone.sid.poller.collector.CollectorFactory;
import es.vodafone.sid.poller.model.CollectorRecord;
import es.vodafone.sid.poller.repository.CollectorRepository;
import es.vodafone.sid.poller.repository.MetricRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SchedulerService implements SchedulingConfigurer {
  private final MetricRepository metricRepository;
  private final List<CollectorsService> collectors;
  public SchedulerService(CollectorRepository collectorRepository, MetricRepository metricRepository) {
    this.metricRepository = metricRepository;
    this.collectors = collectorRepository.findAll().stream()
        .map(this::createCollector)
        .toList();
  }

  private CollectorsService createCollector(CollectorRecord collectorRecord) {
    WorkersService workers = new WorkersService(collectorRecord.workerTimeout(), collectorRecord.name());
    Collector collector = CollectorFactory.create(collectorRecord.protocol(), workers);
    return new CollectorsService(collector, collectorRecord.collectorTimeout(), collectorRecord.name(), collectorRecord.cron(), metricRepository);
  }

  @Override
  public void configureTasks(@NonNull ScheduledTaskRegistrar registrar) {
    collectors.forEach(collector ->
        registrar.addCronTask(collector::collect, collector.getCron())
    );
  }

  @PreDestroy
  public void shutdown() {
    collectors.forEach(CollectorsService::shutdown);
  }
}