package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Source;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
public class ScaledSourceType extends BaseSourceType {

    @Override
    public List<Metric> apply(String rawValue, List<Source> sources, OffsetDateTime instant) {
        Source source = sources.getFirst();
        try {
            BigInteger scaled = new BigDecimal(rawValue.trim())
                .multiply(new BigDecimal(source.scale()))
                .toBigInteger();
            return List.of(metric(source, instant, scaled));
        } catch (NumberFormatException e) {
            log.warn("Could not parse value '{}' for source {}", rawValue, source.name());
            return List.of(nullMetric(source, instant));
        }
    }
}
