package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.SshConfigRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SshConfigRepository {
  private final JdbcTemplate jdbcTemplate;

  private static final RowMapper<SshConfigRecord> ROW_MAPPER = (rs, _) -> new SshConfigRecord(
      rs.getShort("collector_id"),
      rs.getShort("element_type_id"),
      rs.getShort("port"),
      rs.getString("username"),
      rs.getString("password")
  );

  public SshConfigRecord findByElementTypeId(short elementTypeId) {
    return jdbcTemplate.queryForObject(
        "select * from ssh_config where element_type_id = ?", ROW_MAPPER, elementTypeId
    );
  }
}