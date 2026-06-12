package es.vodafone.sid.poller.model;

public record PatternRecord(
    short id,
    short elementTypeId,
    String discoverer,
    short collectorId,
    short netId,
    short grpId,
    short serviceId,
    short serviceTypeId,
    short type,
    short srcType,
    String address,
    String pattern,
    String check,
    String name
) {}