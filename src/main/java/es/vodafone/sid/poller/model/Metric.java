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
    appendField(builder, error, this.value);
    appendInstant(builder, instant);
    return builder.toString();
  }

  private void appendTag(StringBuilder builder, String key, Object value) {
    if (value!=null) builder.append(',').append(key).append('=').append(value);
  }

  private void appendField(StringBuilder builder, Boolean error, BigInteger value) {
    builder.append(" ").append("error=").append(error);
    if(value!=null) builder.append(",").append("value=").append(value).append('u');
  }

  private void appendInstant(StringBuilder builder, Instant instant) {
    builder.append(' ').append(instant.getEpochSecond() * 1_000_000_000L + instant.getNano());
  }
}