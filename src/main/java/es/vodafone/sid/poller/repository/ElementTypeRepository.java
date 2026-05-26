package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.ElementTypeRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ElementTypeRepository {
  private final JdbcTemplate jdbcTemplate;
  public ElementTypeRecord findById(short id) {
    var sql = "select * from element_type where id = ?";
    return jdbcTemplate.queryForObject(sql, (rs, _) -> new ElementTypeRecord(
        rs.getShort("id"),
        rs.getString("name")
    ), id);
  }

  public List<ElementTypeRecord> findAll() {
    var sql = "select * from element_type";
    return jdbcTemplate.query(sql, (rs, _) -> new ElementTypeRecord(
        rs.getShort("id"),
        rs.getString("name")
    ));}

}
