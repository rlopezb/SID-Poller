package es.vodafone.sid.poller.source;

import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Source;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
public class DirectSourceType extends SingleSourceType {

    @Override
    public Metric calculate(String rawValue, Source source, Instant instant) {
        try {
            return metric(source, instant, parse(rawValue));
        } catch (NumberFormatException e) {
            log.warn("Could not parse value '{}' for source {}", rawValue, source.name());
            return nullMetric(source, instant);
        }
    }
}
