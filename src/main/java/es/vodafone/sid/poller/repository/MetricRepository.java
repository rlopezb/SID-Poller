package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.Metric;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MetricRepository {
  private final JdbcTemplate jdbc;
  public void insert(List<Metric> metrics) {
    var sql = """
            INSERT INTO metric (
                instant, src_id, element_id, element_type_id,
                site_id, cdc_id, zone_id, net_id, arch_id,
                group_id, service_id, service_type_id, value
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;    jdbc.batchUpdate(sql, metrics, metrics.size(), (ps, metric) -> {
      ps.setObject(1, metric.instant());
      ps.setShort(2, metric.srcId());
      ps.setShort(3, metric.elementId());
      ps.setShort(4, metric.elementTypeId());
      ps.setShort(5, metric.siteId());
      ps.setShort(6, metric.cdcId());
      ps.setShort(7, metric.zoneId());
      ps.setShort(8, metric.netId());
      ps.setShort(9, metric.archId());
      ps.setShort(10, metric.groupId());
      ps.setShort(11, metric.serviceId());
      ps.setShort(12, metric.serviceTypeId());
      ps.setDouble(13, metric.value());
    });
  }
}
