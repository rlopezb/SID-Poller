package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.Protocol;
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
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final RowMapper<Protocol> ROW_MAPPER = (rs, _) -> {
    JsonNode config = OBJECT_MAPPER.readTree(rs.getString("config"));

    return new Protocol(
        rs.getShort("element_type_id"),
        rs.getString("protocol"),
        config
    );
  };

  public Protocol getByProtocolAndElementTypeId(String protocol, short elementTypeId) {
    return jdbcTemplate.queryForObject(
        "select * from protocol where protocol = ? and element_type_id = ?", ROW_MAPPER, protocol, elementTypeId
    );
  }
}
