package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.SourceRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SourceRepository {
  private final JdbcTemplate jdbcTemplate;

  public List<SourceRecord> findAll() {
    String sql = "select * from source";
    return jdbcTemplate.query(sql, (rs, _) -> new SourceRecord(
        rs.getShort("id"),
        rs.getString("name"),
        rs.getString("description"),
        rs.getShort("element_id"),
        rs.getShort("element_type_id"),
        rs.getShort("site_id"),
        rs.getShort("cdc_id"),
        rs.getShort("zone_id"),
        rs.getShort("net_id"),
        rs.getShort("arch_id"),
        rs.getShort("group_id"),
        rs.getShort("service_id"),
        rs.getShort("service_type_id"),
        rs.getShort("collector_id"),
        rs.getShort("discoverer_id"),
        rs.getString("address"),
        rs.getString("capture"),
        rs.getObject("instant", java.time.OffsetDateTime.class),
        rs.getDouble("cache")
    ));
  }

  public List<SourceRecord> findAllByCollectorId(short collectorId) {
    String sql = "select * from source where collector_id = ?";
    return jdbcTemplate.query(sql, ps -> ps.setShort(1, collectorId), (rs, _) -> new SourceRecord(
        rs.getShort("id"),
        rs.getString("name"),
        rs.getString("description"),
        rs.getShort("element_id"),
        rs.getShort("element_type_id"),
        rs.getShort("site_id"),
        rs.getShort("cdc_id"),
        rs.getShort("zone_id"),
        rs.getShort("net_id"),
        rs.getShort("arch_id"),
        rs.getShort("group_id"),
        rs.getShort("service_id"),
        rs.getShort("service_type_id"),
        rs.getShort("collector_id"),
        rs.getShort("discoverer_id"),
        rs.getString("address"),
        rs.getString("capture"),
        rs.getObject("instant", java.time.OffsetDateTime.class),
        rs.getDouble("cache")
    ));
  }
}