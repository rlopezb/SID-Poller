package es.vodafone.sid.poller.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CollectorsConfiguration {

  @Bean
  @ConfigurationProperties(prefix = "sid.poller.collector.snmp")
  CollectorConfiguration snmpCollectorConfiguration() {
    return new CollectorConfiguration();
  }

  @Bean
  @ConfigurationProperties(prefix = "sid.poller.collector.ssh")
  CollectorConfiguration sshCollectorConfiguration() {
    return new CollectorConfiguration();
  }


}