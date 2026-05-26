package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.ElementRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ElementRepository {
  private final JdbcTemplate jdbcTemplate;

  public ElementRecord findById(short id) {
    var sql = "select * from element where id = ?";
    return jdbcTemplate.queryForObject(sql, (rs, _) -> new ElementRecord(
        rs.getShort("id"),
        rs.getString("name"),
        rs.getShort("element_type_id")
    ), id);
  }

  public List<ElementRecord> findAll() {
    var sql = "select * from element";
    return jdbcTemplate.query(sql, (rs, _) -> new ElementRecord(
        rs.getShort("id"),
        rs.getString("name"),
        rs.getShort("element_type_id")
    ));
  }
}

