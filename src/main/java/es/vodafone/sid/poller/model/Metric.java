package es.vodafone.sid.poller.model;

import java.math.BigInteger;
import java.time.Instant;

public record Metric(
    Instant instant,
    Short srcId,
    Short elementId,
    Short elementTypeId,
    Short siteId,
    Short cdcId,
    Short zoneId,
    Short netId,
    Short archId,
    Short groupId,
    Short serviceId,
    Short serviceTypeId,
    BigInteger value
) {
}