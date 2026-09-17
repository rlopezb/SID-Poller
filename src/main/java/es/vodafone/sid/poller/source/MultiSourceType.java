package es.vodafone.sid.poller.source;

import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Source;

import java.time.Instant;
import java.util.List;

public abstract non-sealed class MultiSourceType extends SourceType {
  public abstract List<Metric> calculate(String rawValue, List<Source> sources, Instant instant);
}
