package es.vodafone.sid.poller.model;

public record SshConfigRecord(
    short collectorId,
    short elementTypeId,
    short maxOid,
    short por,
    String username,
    String password
) {
}
