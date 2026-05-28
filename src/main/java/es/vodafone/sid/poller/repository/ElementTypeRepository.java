package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.ElementTypeRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ElementTypeRepository {
  private final JdbcTemplate jdbcTemplate;

  private static final RowMapper<ElementTypeRecord> ROW_MAPPER = (rs, _) -> new ElementTypeRecord(
      rs.getShort("id"),
      rs.getString("name")
  );

  public ElementTypeRecord findById(short id) {
    return jdbcTemplate.queryForObject("select * from element_type where id = ?", ROW_MAPPER, id);
  }

  public List<ElementTypeRecord> findAll() {
    return jdbcTemplate.query("select * from element_type", ROW_MAPPER);
  }
}
