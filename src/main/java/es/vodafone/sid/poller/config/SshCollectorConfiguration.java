package es.vodafone.sid.poller.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sid.poller.collector.ssh")
public record SshCollectorConfiguration(
    String name,
    String cron,
    long collectorTimeout,
    long workerTimeout,
    int size,
    int queue
) {
}