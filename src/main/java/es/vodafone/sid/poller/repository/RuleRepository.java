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

  private static final RowMapper<Rule> ROW_MAPPER = (rs, k) -> new Rule(
      rs.getObject("id", Short.class),
      rs.getObject("element_type_id", Short.class),
      rs.getObject("discoverer_id", Short.class),
      rs.getObject("collector_id", Short.class),
      rs.getObject("net_id", Short.class),
      rs.getObject("grp_id", Short.class),
      rs.getObject("service_id", Short.class),
      rs.getObject("service_type_id", Short.class),
      rs.getObject("type", Short.class),
      rs.getObject("src_type", Short.class),
      rs.getObject("search", String[].class),
      rs.getObject("pattern", String.class),
      rs.getObject("address", String.class),
      rs.getObject("name", String.class),
      rs.getObject("scale", Short.class)
  );

  public List<Rule> findByDiscovererAndElementTypeId(Short discovererId, Short elementTypeId) {
    return jdbcTemplate.query(
        "select * from rule where discoverer_id = ? and element_type_id = ?",
        ROW_MAPPER, discovererId, elementTypeId
    );
  }
}