package es.vodafone.sid.poller.model;

import java.math.BigInteger;
import java.time.OffsetDateTime;

public record MetricRecord(
    OffsetDateTime instant,
    short srcId,
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
    BigInteger value
) {
}