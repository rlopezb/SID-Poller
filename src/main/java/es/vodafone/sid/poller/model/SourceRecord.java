package es.vodafone.sid.poller.model;

import java.time.OffsetDateTime;

public record SourceRecord(
    short id,
    String name,
    String description,
    short elementId,
    short elementTypeId,
    short siteId,
    short cdcId,
    short zoneId,
    short netId,
    short archId,
    short groupId,
    short serviceId,
    short serviceTypeId,
    short collectorId,
    short discovererId,
    String address,
    String capture,
    OffsetDateTime instant,
    double cache
) {
}
