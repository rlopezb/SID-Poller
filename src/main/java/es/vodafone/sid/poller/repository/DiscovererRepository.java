package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.DiscovererRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DiscovererRepository {
  private final JdbcTemplate jdbcTemplate;

  private static final RowMapper<DiscovererRecord> ROW_MAPPER = (rs, _) -> new DiscovererRecord(
      rs.getShort("id"),
      rs.getString("name"),
      rs.getString("protocol"),
      rs.getString("cron"),
      rs.getInt("discoverer_timeout"),
      rs.getInt("walker_timeout")
  );

  public List<DiscovererRecord> findAll() {
    return jdbcTemplate.query("select * from discoverer", ROW_MAPPER);
  }
}