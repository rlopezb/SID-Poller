package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.configuration.InfluxClient;
import es.vodafone.sid.poller.model.Metric;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MetricRepository {
  private final JdbcTemplate jdbc;
  private final InfluxClient influxClient;
  public void influx(List<Metric> metrics) {

  }

  public void insert(List<Metric> metrics) {
    var sql = """
        INSERT INTO metric (
            instant, src_id, element_id, element_type_id,
            site_id, cdc_id, zone_id, net_id, arch_id,
            group_id, service_id, service_type_id, value
        ) SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
          WHERE EXISTS (SELECT 1 FROM source WHERE id = ? AND active = true)
        """;
    jdbc.batchUpdate(sql, metrics, metrics.size(), (ps, metric) -> {
      ps.setObject(1, metric.instant());
      ps.setObject(2, metric.srcId());
      ps.setObject(3, metric.elementId());
      ps.setObject(4, metric.elementTypeId());
      ps.setObject(5, metric.siteId());
      ps.setObject(6, metric.cdcId());
      ps.setObject(7, metric.zoneId());
      ps.setObject(8, metric.netId());
      ps.setObject(9, metric.archId());
      ps.setObject(10, metric.groupId());
      ps.setObject(11, metric.serviceId());
      ps.setObject(12, metric.serviceTypeId());
      ps.setObject(13, metric.value() == null ? null : metric.value().longValue());
      ps.setObject(14, metric.srcId());
    });
  }
}
