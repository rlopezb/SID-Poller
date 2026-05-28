package es.vodafone.sid.poller.model;

public record SshConfigRecord(
    short elementTypeId,
    short port,
    String username,
    String password,
    long timeout
) {
}
