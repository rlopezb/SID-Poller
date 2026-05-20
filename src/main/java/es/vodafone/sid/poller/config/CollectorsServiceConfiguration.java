package es.vodafone.sid.poller.config;

import es.vodafone.sid.poller.collector.SnmpCollector;
import es.vodafone.sid.poller.collector.SshCollector;
import es.vodafone.sid.poller.service.CollectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CollectorsServiceConfiguration {

  @Bean
  public CollectorService snmpCollectorService(
      SnmpCollector snmpCollector,
      @Qualifier("snmpCollectorConfiguration") CollectorConfiguration config) {
    return new CollectorService(snmpCollector, config.getCollectorTimeout());
  }

  @Bean
  public CollectorService sshCollectorService(
      SshCollector sshCollector,
      @Qualifier("sshCollectorConfiguration") CollectorConfiguration config) {
    return new CollectorService(sshCollector, config.getCollectorTimeout());
  }
}