package es.vodafone.sid.poller.model;

public record Collector(
    Short id,
    String name,
    String protocol,
    String cron,
    Integer collectorTimeout,
    Integer workerTimeout,
    Short size,
    Short queue) {
}
