package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.collector.Collector;
import es.vodafone.sid.poller.collector.CollectorFactory;
import es.vodafone.sid.poller.config.PollerConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SchedulerService implements SchedulingConfigurer {
  private final List<CollectorsService> collectors;
  public SchedulerService(PollerConfiguration properties) {
    this.collectors = properties.getCollectors().stream()
        .map(config -> {
          WorkersService workersService = new WorkersService(config.getWorkerTimeout(), config.getName());
          Collector collector = CollectorFactory.create(config.getProtocol(), workersService);
          return new CollectorsService(collector, config.getCollectorTimeout(), config.getName());
        })
        .toList();
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar registrar) {
    collectors.forEach(collector ->
        registrar.addCronTask(collector::collect, collector.getCron())
    );
  }
}