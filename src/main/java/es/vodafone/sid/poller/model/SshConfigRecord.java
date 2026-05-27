package es.vodafone.sid.poller.model;

public record SshConfigRecord(
    short collectorId,
    short elementTypeId,
    short port,
    String username,
    String password
) {
}
