package es.vodafone.sid.poller.model;

import es.vodafone.sid.poller.strategy.SourceTypeRegistry;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;

public record Source(
    Short id,
    String name,
    String description,
    Short type,
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
    Short collectorId,
    Short discovererId,
    String address,
    String capture,
    Instant instant,
    BigInteger cache,
    Integer scale,
    Boolean active
) {
  public boolean isMulti() {
    return type == SourceTypeRegistry.getMulti();
  }

  public boolean isSame(Source other) {
    return this.name().equals(other.name())
        && Objects.equals(this.description(), other.description())
        && Objects.equals(this.type(), other.type())
        && Objects.equals(this.elementId(), other.elementId())
        && Objects.equals(this.elementTypeId(), other.elementTypeId())
        && Objects.equals(this.siteId(), other.siteId())
        && Objects.equals(this.cdcId(), other.cdcId())
        && Objects.equals(this.zoneId(), other.zoneId())
        && Objects.equals(this.netId(), other.netId())
        && Objects.equals(this.archId(), other.archId())
        && Objects.equals(this.groupId(), other.groupId())
        && Objects.equals(this.serviceId(), other.serviceId())
        && Objects.equals(this.serviceTypeId(), other.serviceTypeId())
        && Objects.equals(this.collectorId(), other.collectorId())
        && Objects.equals(this.discovererId(), other.discovererId())
        && Objects.equals(this.address(),other.address())
        && Objects.equals(this.capture(), other.capture())
        && Objects.equals(this.scale(), other.scale());
  }
}
