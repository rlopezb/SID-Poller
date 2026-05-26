package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.CollectorRecord;
import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.repository.CollectorRepository;
import es.vodafone.sid.poller.repository.MetricRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService implements SchedulingConfigurer {
  private final CollectorFactory collectorFactory;
  private final MetricRepository metricRepository;
  private final CollectorRepository collectorRepository;
  private List<CollectorService> collectorServices;

  private CollectorService createCollectorService(CollectorRecord collectorRecord) {
    WorkersService workersService = new WorkersService(collectorRecord.workerTimeout(), collectorRecord.name());
    Callable<List<Metric>> collector = collectorFactory.create(collectorRecord.protocol(), workersService);
    return new CollectorService(
        collector,
        workersService,
        collectorRecord,
        metricRepository);
  }

  @Override
  public void configureTasks(@NonNull ScheduledTaskRegistrar registrar) {
    this.collectorServices = collectorRepository.findAll().stream()
        .map(this::createCollectorService)
        .toList();
    collectorServices.forEach(collector ->
        registrar.addCronTask(collector::collect, collector.getCollectorRecord().cron())
    );
  }

  @PreDestroy
  public void shutdown() {
    collectorServices.forEach(CollectorService::shutdown);
  }
}