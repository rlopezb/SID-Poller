package es.vodafone.sid.poller.model;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class MetricRecords {

  private MetricRecords() {
  }

  public static MetricRecord metric(SourceRecord source, OffsetDateTime instant, BigInteger value) {
    return new MetricRecord(
        instant,
        source.id(), source.elementId(), source.elementTypeId(),
        source.siteId(), source.cdcId(), source.zoneId(), source.netId(),
        source.archId(), source.groupId(), source.serviceId(), source.serviceTypeId(),
        value
    );
  }

  public static MetricRecord nullValue(SourceRecord source, OffsetDateTime instant) {
    return metric(source, instant, null);
  }

  public static List<MetricRecord> nullValues(List<SourceRecord> sources, OffsetDateTime instant) {
    return sources.stream()
        .map(source -> nullValue(source, instant))
        .toList();
  }

  public static List<MetricRecord> complete(
      List<SourceRecord> sources,
      List<MetricRecord> metrics,
      OffsetDateTime instant
  ) {
    Set<Short> sourceIds = sources.stream()
        .map(SourceRecord::id)
        .collect(Collectors.toCollection(HashSet::new));

    Map<Short, MetricRecord> metricsBySource = metrics == null
        ? Map.of()
        : metrics.stream()
        .filter(metric -> metric != null && sourceIds.contains(metric.srcId()))
        .collect(Collectors.toMap(
            MetricRecord::srcId,
            Function.identity(),
            MetricRecords::preferMetricWithValue
        ));

    List<MetricRecord> completed = new ArrayList<>(sources.size());
    for (SourceRecord source : sources) {
      completed.add(metricsBySource.getOrDefault(source.id(), nullValue(source, instant)));
    }
    return completed;
  }

  private static MetricRecord preferMetricWithValue(MetricRecord current, MetricRecord candidate) {
    return current.value() == null && candidate.value() != null ? candidate : current;
  }
}
