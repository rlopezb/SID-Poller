package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.Rule;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RuleRepository {
  private final JdbcTemplate jdbcTemplate;

  private static final RowMapper<Rule> ROW_MAPPER = (rs, _) -> new Rule(
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
      rs.getString("name"),
      rs.getShort("scale")
  );

  public List<Rule> findByDiscovererAndElementTypeId(String discoverer, short elementTypeId) {
    return jdbcTemplate.query(
        "select * from rule where discoverer = ? and element_type_id = ?",
        ROW_MAPPER, discoverer, elementTypeId
    );
  }
}