package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.CollectorRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CollectorRepository {
  private final JdbcTemplate jdbcTemplate;
  public List<CollectorRecord> findAll() {
    var sql = "select * from collector";
    return jdbcTemplate.query(sql, (rs, _) -> new CollectorRecord(
        rs.getShort("id"),
        rs.getString("name"),
        rs.getString("protocol"),
        rs.getString("cron"),
        rs.getLong("collector_timeout"),
        rs.getLong("worker_timeout"),
        rs.getShort("size"),
        rs.getShort("queue")
    ));
  }
}