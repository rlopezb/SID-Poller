package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.PatternRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PatternRepository {
  private final JdbcTemplate jdbcTemplate;

  private static final RowMapper<PatternRecord> ROW_MAPPER = (rs, _) -> new PatternRecord(
      rs.getShort("id"),
      rs.getShort("element_type_id"),
      rs.getString("discoverer"),
      rs.getShort("collector_id"),
      rs.getShort("net_id"),
      rs.getShort("grp_id"),
      rs.getShort("service_id"),
      rs.getShort("service_type_id"),
      rs.getShort("type"),
      rs.getShort("src_type"),
      rs.getString("address"),
      rs.getString("pattern"),
      rs.getString("check"),
      rs.getString("name")
  );

  public List<PatternRecord> findByDiscovererAndElementTypeId(String discoverer, short elementTypeId) {
    return jdbcTemplate.query(
        "select * from pattern where discoverer = ? and element_type_id = ?",
        ROW_MAPPER, discoverer, elementTypeId
    );
  }
}