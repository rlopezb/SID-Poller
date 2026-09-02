package es.vodafone.sid.poller.worker;

import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Source;
import es.vodafone.sid.poller.strategy.BaseSourceType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public interface Worker extends Callable<List<Metric>> {
  List<Source> getSources();
  default List<Metric> buildMetrics(List<Source> sources, Map<Short, Metric> metricMap, OffsetDateTime instant) {
    List<Metric> metrics = new ArrayList<>();
    for (Source source : sources) {
      metrics.add(metricMap.getOrDefault(source.id(), BaseSourceType.nullMetric(source, instant)));
    }
    return metrics;
  }
}
