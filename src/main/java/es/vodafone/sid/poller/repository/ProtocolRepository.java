package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.ProtocolRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;

@Repository
@RequiredArgsConstructor
public class ProtocolRepository {
  private final JdbcTemplate jdbcTemplate;
  private static final RowMapper<ProtocolRecord> ROW_MAPPER = (rs, _) -> new ProtocolRecord(
      rs.getShort("element_type_id"),
      rs.getString("protocol"),
      rs.getObject("config", JsonNode.class)

  );

  public ProtocolRecord getByProtocol(String protocol) {
    return jdbcTemplate.queryForObject(
        "select * from protocol where protocol = ?", ROW_MAPPER, protocol
    );
  }

   public ProtocolRecord getByElementTypeId(short elementTypeId) {
    return jdbcTemplate.queryForObject(
        "select * from protocol where element_type_id = ?", ROW_MAPPER, elementTypeId
    );
  }
}
