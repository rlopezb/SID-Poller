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
  @Data
  public static class CollectorConfiguration {
    private String name;
    private String protocol;
    private String cron;
    private long collectorTimeout;
    private long workerTimeout;
    private int size;
    private int queue;
  }
  private List<CollectorConfiguration> collectors = new ArrayList<>();
}
