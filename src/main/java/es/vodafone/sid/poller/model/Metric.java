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
    Boolean error,
    BigInteger value
) {
  public String map(){
    if(value == null) return null;
    StringBuilder builder = new StringBuilder();
    builder.setLength(0);
    builder.append("metric");
    appendTag(builder, "srcId", this.srcId);
    appendTag(builder, "elementId", this.elementId);
    appendTag(builder, "elementTypeId", this.elementTypeId);
    appendTag(builder, "siteId", this.siteId);
    appendTag(builder, "cdcId", this.cdcId);
    appendTag(builder, "zoneId", this.zoneId);
    appendTag(builder, "netId", this.netId);
    appendTag(builder, "archId", this.archId);
    appendTag(builder, "groupId", this.groupId);
    appendTag(builder, "serviceId", this.serviceId);
    appendTag(builder, "serviceTypeId", this.serviceTypeId);
    appendTag(builder, "error", this.error);
    builder.append(' ');
    appendValue(builder, this.value());
    builder.append(' ').append(toEpochNanos(this.instant()));
    return builder.toString();
  }
  private void appendTag(StringBuilder builder, String key, Object value) {
    if (value!=null) builder.append(',').append(key).append('=').append(value);
  }
  private void appendValue(StringBuilder builder, BigInteger value) {
    builder.append("value=").append(value).append('u');
  }
  private long toEpochNanos(Instant instant) {
    return instant.getEpochSecond() * 1_000_000_000L + instant.getNano();
  }
}