package es.vodafone.sid.poller.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "sid.poller")
@Data
@Validated
public class PollerConfiguration {
  @Data
  public static class CollectorConfiguration {
    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotBlank(message = "Protocol cannot be blank")
    private String protocol;

    @Pattern(regexp = "^(?:[0-5]\\d|\\*|\\?.*|.*?-.*?|.*?/.*?).*",
        message = "Invalid cron expression")
    private String cron;

    @Positive(message = "collectorTimeout must be positive")
    private long collectorTimeout;

    @Positive(message = "workerTimeout must be positive")
    private long workerTimeout;

    @Positive(message = "size must be positive")
    private int size;

    @PositiveOrZero(message = "queue must be >= 0")
    private int queue;
  }
  private List<CollectorConfiguration> collectors = new ArrayList<>();
}
