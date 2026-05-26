package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.ElementRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ElementRepository {
  private final JdbcTemplate jdbcTemplate;

  private static final RowMapper<ElementRecord> ROW_MAPPER = (rs, _) -> new ElementRecord(
      rs.getShort("id"),
      rs.getString("name"),
      rs.getShort("element_type_id")
  );

  public ElementRecord findById(short id) {
    return jdbcTemplate.queryForObject("select * from element where id = ?", ROW_MAPPER, id);
  }

  public List<ElementRecord> findAll() {
    return jdbcTemplate.query("select * from element", ROW_MAPPER);
  }
}

