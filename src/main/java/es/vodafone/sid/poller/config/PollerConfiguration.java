package es.vodafone.sid.poller.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "sid.poller")
@Data
public class PollerConfiguration {
  private List<CollectorConfiguration> collectors = new ArrayList<>();
}
