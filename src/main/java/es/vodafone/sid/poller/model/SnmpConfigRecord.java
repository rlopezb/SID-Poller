package es.vodafone.sid.poller.model;

public record SnmpConfigRecord(
    short elementTypeId,
    short maxOid,
    short port,
    String username,
    String authProtocol,
    String authPassword,
    String privProtocol,
    String privPassword,
    String securityLevel
) {}


