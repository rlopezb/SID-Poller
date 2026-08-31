package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.Source;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SourceRepository {
  private final JdbcTemplate jdbcTemplate;
  private static final RowMapper<Source> ROW_MAPPER = (rs, _) -> new Source(
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
      rs.getObject("cache", java.math.BigDecimal.class) != null
          ? rs.getObject("cache", java.math.BigDecimal.class).toBigInteger()
          : null,
      rs.getDouble("scale"),
      rs.getBoolean("active")
  );
  public List<Source> findAll() {
    return jdbcTemplate.query("select * from source", ROW_MAPPER);
  }

  public List<Source> findByCollectorId(short collectorId) {
    return jdbcTemplate.query("select * from source where collector_id = ? and active = true", ROW_MAPPER, collectorId);
  }

  public List<Source> findByElementIdAndCollectorId(short elementId, short collectorId) {
    return jdbcTemplate.query("select * from source where collector_id = ? and element_id = ? ", ROW_MAPPER, collectorId, elementId);
  }

  public List<Source> findByElementIdAndCollectorIdAndDiscovererId(short elementId, short collectorId, short discovererId) {
    return jdbcTemplate.query("select * from source where collector_id = ? and element_id = ?  and discoverer_id = ?", ROW_MAPPER, collectorId, elementId, discovererId);
  }

  public void insert(Source source) {
    jdbcTemplate.update("""
        insert into source (
            name, description, type, element_id, element_type_id,
            site_id, cdc_id, zone_id, net_id, arch_id,
            group_id, service_id, service_type_id,
            collector_id, discoverer_id, address, capture, instant, cache, scale, active
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        source.name(), source.description(), source.type(),
        source.elementId(), source.elementTypeId(),
        source.siteId(), source.cdcId(), source.zoneId(), source.netId(), source.archId(),
        source.groupId(), source.serviceId(), source.serviceTypeId(),
        source.collectorId(), source.discovererId(),
        source.address(), source.capture(), source.instant(), source.cache(), source.scale(), source.active()
    );
  }

  public boolean hasMetrics(short sourceId) {
    return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
        "select exists (select 1 from metric where src_id = ?)", Boolean.class, sourceId));
  }

  public void setActive(short id, boolean active) {
    jdbcTemplate.update(
        "update source set active = ? where id = ? and active is distinct from ?", active, id, active);
  }

  public void deleteById(short id) {
    jdbcTemplate.update("delete from source where id = ?", id);
  }

  public void updateCacheAndInstant(short id, BigInteger cache, OffsetDateTime instant) {
    jdbcTemplate.update("update source set cache = ?, instant = ? where id = ?", new BigDecimal(cache), instant, id);
  }
}
