package es.vodafone.sid.poller.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sid.poller.worker.snmp")
record SnmpWorkerConfiguration(
    String name,
    long timeout,
    int size
) {
}
