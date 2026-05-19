package es.vodafone.sid.poller.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sid.poller.collector.snmp")
public record SnmpCollectorConfiguration(
    String name,
    String cron,
    long collectorTimeout,
    long workerTimeout,
    int size,
    int queue
) {
}