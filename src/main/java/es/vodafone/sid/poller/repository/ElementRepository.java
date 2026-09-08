package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.Element;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ElementRepository {
  private final JdbcTemplate jdbcTemplate;

  private static final RowMapper<Element> ROW_MAPPER = (rs, _) -> new Element(
      rs.getObject("id", Short.class),
      rs.getObject("name", String.class),
      rs.getObject("element_type_id", Short.class),
      rs.getObject("site_id", Short.class),
      rs.getObject("cdc_id", Short.class),
      rs.getObject("zone_id", Short.class),
      rs.getObject("arch_id", Short.class),
      rs.getObject("net_id", Short.class)
  );

  public Element findById(Short id) {
    return jdbcTemplate.queryForObject("select * from element where id = ?", ROW_MAPPER, id);
  }

  public List<Element> findAll() {
    return jdbcTemplate.query("select * from element", ROW_MAPPER);
  }
}

