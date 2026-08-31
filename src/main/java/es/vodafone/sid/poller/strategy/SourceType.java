package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Source;

import java.time.OffsetDateTime;
import java.util.List;

public interface SourceType {
    List<Metric> apply(String rawValue, List<Source> sources, OffsetDateTime instant);
}
