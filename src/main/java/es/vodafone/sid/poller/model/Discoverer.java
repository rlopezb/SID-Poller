package es.vodafone.sid.poller.model;

public record Discoverer(
    short id,
    String name,
    String protocol,
    String cron,
    int discovererTimeout,
    int workerTimeout
) {}