package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.SourceRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SourceRepository {
  private final JdbcTemplate jdbcTemplate;
  private static final RowMapper<SourceRecord> ROW_MAPPER = (rs, _) -> new SourceRecord(
      rs.getShort("id"),
      rs.getString("name"),
      rs.getString("description"),
      rs.getShort("type"),
      rs.getShort("element_id"),
      rs.getShort("element_type_id"),
      rs.getShort("site_id"),
      rs.getShort("cdc_id"),
      rs.getShort("zone_id"),
      rs.getShort("net_id"),
      rs.getShort("arch_id"),
      rs.getShort("group_id"),
      rs.getShort("service_id"),
      rs.getShort("service_type_id"),
      rs.getShort("collector_id"),
      rs.getShort("discoverer_id"),
      rs.getString("address"),
      rs.getString("capture"),
      rs.getObject("instant", OffsetDateTime.class),
      rs.getDouble("cache")
  );
  public List<SourceRecord> findAll() {
    return jdbcTemplate.query("select * from source", ROW_MAPPER);
  }

  public List<SourceRecord> findByCollectorId(short collectorId) {
    return jdbcTemplate.query("select * from source where collector_id = ?", ROW_MAPPER, collectorId);
  }

  public List<SourceRecord> findByElementIdAndCollectorId(short elementId, short collectorId) {
    return jdbcTemplate.query("select * from source where collector_id = ? and element_id = ? ", ROW_MAPPER, collectorId, elementId);
  }

  public void insert(SourceRecord sourceRecord) {
    jdbcTemplate.update("""
        insert into source (
            name, description, type, element_id, element_type_id,
            site_id, cdc_id, zone_id, net_id, arch_id,
            group_id, service_id, service_type_id,
            collector_id, discoverer_id, address, capture
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        sourceRecord.name(), sourceRecord.description(), sourceRecord.type(),
        sourceRecord.elementId(), sourceRecord.elementTypeId(),
        sourceRecord.siteId(), sourceRecord.cdcId(), sourceRecord.zoneId(), sourceRecord.netId(), sourceRecord.archId(),
        sourceRecord.groupId(), sourceRecord.serviceId(), sourceRecord.serviceTypeId(),
        sourceRecord.collectorId(), sourceRecord.discovererId(),
        sourceRecord.address(), sourceRecord.capture()
    );
  }

  public void deleteById(short id) {
    jdbcTemplate.update("delete from source where id = ?", id);
  }

  public void updateCacheAndInstant(short id, double cache, OffsetDateTime instant) {
    jdbcTemplate.update("update source set cache = ?, instant = ? where id = ?", cache, instant, id);
  }
}