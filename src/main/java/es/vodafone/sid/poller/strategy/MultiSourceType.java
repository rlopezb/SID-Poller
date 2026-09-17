package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Source;

import java.time.Instant;
import java.util.List;

public abstract class MultiSourceType extends SourceType {
  protected Boolean isMulti(){
    return true;
  }
  public abstract List<Metric> calculate(String rawValue, List<Source> sources, Instant instant);
}
