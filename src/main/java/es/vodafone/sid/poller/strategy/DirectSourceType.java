package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.model.SourceRecord;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
public class DirectSourceType extends BaseSourceType {

    @Override
    public List<MetricRecord> apply(String rawValue, List<SourceRecord> sources, OffsetDateTime instant) {
        SourceRecord source = sources.getFirst();
        try {
            return List.of(metric(source, instant, parse(rawValue)));
        } catch (NumberFormatException e) {
            log.warn("Could not parse value '{}' for source {}", rawValue, source.name());
            return List.of(BaseSourceType.nullMetric(source, instant));
        }
    }
}
