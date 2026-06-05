package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.model.SourceRecord;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class SumScaledSourceType extends BaseSourceType {

    @Override
    public List<MetricRecord> apply(String rawValue, List<SourceRecord> sources, OffsetDateTime instant) {
        SourceRecord source = sources.getFirst();
        try {
            BigDecimal sum = Arrays.stream(rawValue.split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(BigDecimal::new)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigInteger scaled = sum.multiply(BigDecimal.valueOf(source.cache())).toBigInteger();
            return List.of(metric(source, instant, scaled));
        } catch (NumberFormatException e) {
            log.warn("Could not parse sum scaled value '{}' for source {}", rawValue, source.name());
            return List.of(nullMetric(source, instant));
        }
    }
}
