package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.MetricRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MetricRepository {
  private final JdbcTemplate jdbc;
  public void insert(List<MetricRecord> metricRecords) {
    var sql = """
            INSERT INTO metric (
                instant, src_id, element_id, element_type_id,
                site_id, cdc_id, zone_id, net_id, arch_id,
                group_id, service_id, service_type_id, value
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;    jdbc.batchUpdate(sql, metricRecords, metricRecords.size(), (ps, metricRecord) -> {
      ps.setObject(1, metricRecord.instant());
      ps.setShort(2, metricRecord.srcId());
      ps.setShort(3, metricRecord.elementId());
      ps.setShort(4, metricRecord.elementTypeId());
      ps.setShort(5, metricRecord.siteId());
      ps.setShort(6, metricRecord.cdcId());
      ps.setShort(7, metricRecord.zoneId());
      ps.setShort(8, metricRecord.netId());
      ps.setShort(9, metricRecord.archId());
      ps.setShort(10, metricRecord.groupId());
      ps.setShort(11, metricRecord.serviceId());
      ps.setShort(12, metricRecord.serviceTypeId());
      ps.setDouble(13, metricRecord.value());
    });
  }
}
