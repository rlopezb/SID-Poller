package es.vodafone.sid.poller.model;

public record Rule(
    Short id,
    Short elementTypeId,
    Short discovererId,
    Short collectorId,
    Short netId,
    Short grpId,
    Short serviceId,
    Short serviceTypeId,
    Short type,
    Short srcType,
    String[] search,
    String pattern,
    String address,
    String name,
    Short scale
) {}