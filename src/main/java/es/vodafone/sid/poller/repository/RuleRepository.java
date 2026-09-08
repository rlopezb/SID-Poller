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
      rs.getObject("id", Short.class),
      rs.getObject("element_type_id", Short.class),
      rs.getObject("discoverer", String.class),
      rs.getObject("collector_id", Short.class),
      rs.getObject("net_id", Short.class),
      rs.getObject("grp_id", Short.class),
      rs.getObject("service_id", Short.class),
      rs.getObject("service_type_id", Short.class),
      rs.getObject("type", Short.class),
      rs.getObject("src_type", Short.class),
      rs.getObject("address", String.class),
      rs.getObject("pattern", String.class),
      rs.getObject("check", String.class),
      rs.getObject("name", String.class),
      rs.getObject("scale", Integer.class)
  );

  public List<Rule> findByDiscovererAndElementTypeId(String discoverer, Short elementTypeId) {
    return jdbcTemplate.query(
        "select * from rule where discoverer = ? and element_type_id = ?",
        ROW_MAPPER, discoverer, elementTypeId
    );
  }
}