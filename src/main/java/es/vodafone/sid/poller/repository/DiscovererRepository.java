package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.Discoverer;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DiscovererRepository {
  private final JdbcTemplate jdbcTemplate;

  private static final RowMapper<Discoverer> ROW_MAPPER = (rs, _) -> new Discoverer(
      rs.getObject("id", Short.class),
      rs.getObject("name", String.class),
      rs.getObject("protocol", String.class),
      rs.getObject("cron", String.class),
      rs.getObject("discoverer_timeout", Integer.class),
      rs.getObject("walker_timeout", Integer.class)
  );

  public List<Discoverer> findAll() {
    return jdbcTemplate.query("select * from discoverer", ROW_MAPPER);
  }
}