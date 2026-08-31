package es.vodafone.sid.poller.model;

import es.vodafone.sid.poller.strategy.SourceTypeRegistry;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.Objects;

public record Source(
    short id,
    String name,
    String description,
    short type,
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
    BigInteger cache,
    double scale,
    boolean active
) {
  public boolean isMulti() {
    return type == SourceTypeRegistry.getMulti();
  }

  public boolean isSame(Source other) {
    return this.name().equals(other.name())
        && Objects.equals(this.description(), other.description())
        && this.type() == other.type()
        && this.elementId() == other.elementId()
        && this.elementTypeId() == other.elementTypeId()
        && this.siteId() == other.siteId()
        && this.cdcId() == other.cdcId()
        && this.zoneId() == other.zoneId()
        && this.netId() == other.netId()
        && this.archId() == other.archId()
        && this.groupId() == other.groupId()
        && this.serviceId() == other.serviceId()
        && this.serviceTypeId() == other.serviceTypeId()
        && this.collectorId() == other.collectorId()
        && this.discovererId() == other.discovererId()
        && this.address().equals(other.address())
        && Objects.equals(this.capture(), other.capture())
        && this.scale() == other.scale();
  }
}
