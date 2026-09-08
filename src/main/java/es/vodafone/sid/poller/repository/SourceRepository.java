package es.vodafone.sid.poller.repository;

import es.vodafone.sid.poller.model.Source;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SourceRepository {
  private final JdbcTemplate jdbcTemplate;
  private static final RowMapper<Source> ROW_MAPPER = (rs, _) -> new Source(
      rs.getObject("id", Short.class),
      rs.getObject("name", String.class),
      rs.getObject("description", String.class),
      rs.getObject("type", Short.class),
      rs.getObject("element_id", Short.class),
      rs.getObject("element_type_id", Short.class),
      rs.getObject("site_id", Short.class),
      rs.getObject("cdc_id", Short.class),
      rs.getObject("zone_id", Short.class),
      rs.getObject("net_id", Short.class),
      rs.getObject("arch_id", Short.class),
      rs.getObject("group_id", Short.class),
      rs.getObject("service_id", Short.class),
      rs.getObject("service_type_id", Short.class),
      rs.getObject("collector_id", Short.class),
      rs.getObject("discoverer_id", Short.class),
      rs.getObject("address", String.class),
      rs.getObject("capture", String.class),
      rs.getObject("instant", Instant.class),
      rs.getObject("cache", Long.class) == null ?
          new BigInteger(Long.toUnsignedString(rs.getObject("cache", Long.class))) :
          null,
      rs.getObject("scale", Integer.class),
      rs.getObject("active", Boolean.class)
  );

  public List<Source> findAll() {
    return jdbcTemplate.query("select * from source", ROW_MAPPER);
  }

  public List<Source> findByCollectorId(Short collectorId) {
    return jdbcTemplate.query("select * from source where collector_id = ? and active = true", ROW_MAPPER, collectorId);
  }

  public List<Source> findByElementIdAndCollectorId(Short elementId, Short collectorId) {
    return jdbcTemplate.query("select * from source where collector_id = ? and element_id = ? ", ROW_MAPPER, collectorId, elementId);
  }

  public List<Source> findByElementIdAndCollectorIdAndDiscovererId(Short elementId, Short collectorId, Short discovererId) {
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
        source.address(), source.capture(), source.instant(), source.cache().longValue(), source.scale(), source.active()
    );
  }

  public boolean hasMetrics(Short sourceId) {
    return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
        "select exists (select 1 from metric where src_id = ?)", Boolean.class, sourceId));
  }

  public void setActive(Short id, Boolean active) {
    jdbcTemplate.update(
        "update source set active = ? where id = ? and active is distinct from ?", active, id, active);
  }

  public void deleteById(Short id) {
    jdbcTemplate.update("delete from source where id = ?", id);
  }

  public void updateCacheAndInstant(Short id, BigInteger cache, Instant instant) {

    jdbcTemplate.update("update source set cache = ?, instant = ? where id = ?", cache == null ? null : cache.longValue(), instant, id);
  }
}
