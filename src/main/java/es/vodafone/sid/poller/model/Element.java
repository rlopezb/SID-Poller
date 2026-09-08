package es.vodafone.sid.poller.model;

public record Element(
    Short id,
    String name,
    Short elementTypeId,
    Short siteId,
    Short cdcId,
    Short zoneId,
    Short archId,
    Short netId
) {
}
