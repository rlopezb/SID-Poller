package es.vodafone.sid.poller.model;

public record ElementRecord(
    short id,
    String name,
    short elementTypeId,
    short siteId,
    short cdcId,
    short zoneId,
    short archId,
    short netId
) {
}
