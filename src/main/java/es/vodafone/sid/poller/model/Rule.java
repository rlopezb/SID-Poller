package es.vodafone.sid.poller.model;

public record Rule(
    Short id,
    Short elementTypeId,
    String discoverer,
    Short collectorId,
    Short netId,
    Short grpId,
    Short serviceId,
    Short serviceTypeId,
    Short type,
    Short srcType,
    String address,
    String pattern,
    String search,
    String name,
    Integer scale
) {}