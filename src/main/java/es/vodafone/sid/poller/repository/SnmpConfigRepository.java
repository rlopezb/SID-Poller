package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.SnmpConfigRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SnmpConfigRepository {
  private final JdbcTemplate jdbcTemplate;

  private static final RowMapper<SnmpConfigRecord> ROW_MAPPER = (rs, _) -> new SnmpConfigRecord(
      rs.getShort("element_type_id"),
      rs.getShort("max_oid"),
      rs.getShort("port"),
      rs.getString("username"),
      rs.getString("auth_protocol"),
      rs.getString("auth_password"),
      rs.getString("priv_protocol"),
      rs.getString("priv_password"),
      rs.getString("security_level")
  );

  public SnmpConfigRecord findByElementTypeId(short elementTypeId) {
    return jdbcTemplate.queryForObject(
        "select * from snmp_config where element_type_id = ?", ROW_MAPPER, elementTypeId
    );
  }
}