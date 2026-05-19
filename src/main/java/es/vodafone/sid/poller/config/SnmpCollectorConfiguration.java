package es.vodafone.sid.poller.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

@ConfigurationProperties(prefix = "sid.poller.collector.snmp")
public record SnmpCollectorConfiguration (
    String name,
    String cron,
    long timeout,
    int size,
    int queue
) {
}