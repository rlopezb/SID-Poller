package es.vodafone.sid.poller.model;

public record CollectorRecord(
    short id,
    String name,
    String protocol,
    String cron,
    int collectorTimeout,
    int workerTimeout,
    short size,
    short queue) {
}
