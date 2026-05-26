package es.vodafone.sid.poller.model;

public record CollectorRecord(
    short id,
    String name,
    String protocol,
    String cron,
    long collectorTimeout,
    long workerTimeout,
    short size,
    short queue) {
}
