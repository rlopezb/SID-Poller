package es.vodafone.sid.poller.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sid.poller.worker.ssh")
record SshWorkerConfiguration(
    String name,
    long timeout,
    int size
) {
}
