package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.ProtocolRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class ProtocolRepository {
  private final JdbcTemplate jdbcTemplate;
  private static final RowMapper<ProtocolRecord> ROW_MAPPER = (rs, _) -> {
    ObjectMapper mapper = new ObjectMapper();
    String json = rs.getString("config");
    JsonNode config = mapper.readTree(json);

    return new ProtocolRecord(
        rs.getShort("element_type_id"),
        rs.getString("protocol"),
        config
    );
  };

  public ProtocolRecord getByProtocolAndElementTypeId(String protocol, short elementTypeId) {
    return jdbcTemplate.queryForObject(
        "select * from protocol where protocol = ? and element_type_id = ?", ROW_MAPPER, protocol, elementTypeId
    );
  }
}
