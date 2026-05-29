// worker/SshWorker.java
package es.vodafone.sid.poller.worker;

import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.model.ProtocolRecord;
import es.vodafone.sid.poller.model.SourceRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
public class SshWorker implements Callable<List<MetricRecord>> {

  private final List<SourceRecord> sources;
  private final ProtocolRecord protocol;

  @Override
  public List<MetricRecord> call() {
    log.debug("SSH worker executing {} commands on element {}",
        sources.size(), sources.getFirst().elementId());

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    return sources.stream()
        .map(source -> new MetricRecord(
            now,
            source.id(),
            source.elementId(),
            source.elementTypeId(),
            source.siteId(),
            source.cdcId(),
            source.zoneId(),
            source.netId(),
            source.archId(),
            source.groupId(),
            source.serviceId(),
            source.serviceTypeId(),
            BigInteger.valueOf(java.util.concurrent.ThreadLocalRandom.current().nextLong(10_000_001))
        ))
        .toList();
  }
}