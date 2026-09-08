package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.Collector;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CollectorRepository {
  private final JdbcTemplate jdbcTemplate;

  private static final RowMapper<Collector> ROW_MAPPER = (rs, _) -> new Collector(
      rs.getObject("id", Short.class),
      rs.getObject("name", String.class),
      rs.getObject("protocol", String.class),
      rs.getObject("cron", String.class),
      rs.getObject("collector_timeout", Integer.class),
      rs.getObject("worker_timeout", Integer.class),
      rs.getObject("size", Short.class),
      rs.getObject("queue", Short.class)
  );

  public List<Collector> findAll() {
    var sql = "select * from collector";
    return jdbcTemplate.query(sql, ROW_MAPPER);
  }
}