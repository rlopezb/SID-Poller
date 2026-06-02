package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.model.SourceRecord;

import java.time.OffsetDateTime;
import java.util.List;

public interface SourceType {
    List<MetricRecord> apply(String rawValue, List<SourceRecord> sources, OffsetDateTime instant);
}
