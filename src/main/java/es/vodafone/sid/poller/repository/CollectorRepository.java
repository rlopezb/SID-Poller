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
      rs.getShort("id"),
      rs.getString("name"),
      rs.getString("protocol"),
      rs.getString("cron"),
      rs.getInt("collector_timeout"),
      rs.getInt("worker_timeout"),
      rs.getShort("size"),
      rs.getShort("queue")
  );

  public List<Collector> findAll() {
    var sql = "select * from collector";
    return jdbcTemplate.query(sql, ROW_MAPPER);
  }
}