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
      rs.getShort("id"),
      rs.getString("name"),
      rs.getShort("element_type_id"),
      rs.getShort("site_id"),
      rs.getShort("cdc_id"),
      rs.getShort("zone_id"),
      rs.getShort("arch_id"),
      rs.getShort("net_id")

  );

  public Element findById(short id) {
    return jdbcTemplate.queryForObject("select * from element where id = ?", ROW_MAPPER, id);
  }

  public List<Element> findAll() {
    return jdbcTemplate.query("select * from element", ROW_MAPPER);
  }
}

