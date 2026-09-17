package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Source;

import java.time.Instant;

public abstract non-sealed class SingleSourceType extends SourceType {
  public abstract Metric calculate(String rawValue, Source source, Instant instant);
}
