package es.vodafone.sid.poller.config;

import es.vodafone.sid.poller.service.WorkersService;
import es.vodafone.sid.poller.worker.SnmpWorker;
import es.vodafone.sid.poller.worker.SshWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class WorkersServiceConfiguration {

  @Bean
  public WorkersService<SnmpWorker> snmpWorkersService(@Qualifier("snmpCollectorConfiguration") CollectorConfiguration config) {
    return new WorkersService<>(config.getWorkerTimeout());
  }

  @Bean
  public WorkersService<SshWorker> sshWorkersService(@Qualifier("sshCollectorConfiguration") CollectorConfiguration config) {
    return new WorkersService<>(config.getWorkerTimeout());
  }
}