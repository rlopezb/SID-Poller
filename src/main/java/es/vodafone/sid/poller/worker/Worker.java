package es.vodafone.sid.poller.worker;

import es.vodafone.sid.poller.model.Element;
import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Protocol;
import es.vodafone.sid.poller.model.Source;
import es.vodafone.sid.poller.strategy.BaseSourceType;
import es.vodafone.sid.poller.strategy.SourceTypeRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@RequiredArgsConstructor
public abstract class Worker implements Callable<List<Metric>> {
  public final Element element;
  @Getter
  public final List<Source> sources;
  public final Protocol protocol;
  public final SourceTypeRegistry sourceTypeRegistry;

  public List<Metric> buildMetrics(List<Source> sources, Map<Short, Metric> metricMap, Instant instant) {
    List<Metric> metrics = new ArrayList<>();
    for (Source source : sources) {
      metrics.add(metricMap.getOrDefault(source.id(), BaseSourceType.nullMetric(source, instant)));
    }
    return metrics;
  }
}
