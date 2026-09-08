package es.vodafone.sid.poller.model;

public record Discoverer(
    Short id,
    String name,
    String protocol,
    String cron,
    Integer discovererTimeout,
    Integer workerTimeout
) {}