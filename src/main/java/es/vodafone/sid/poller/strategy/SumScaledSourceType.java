package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Source;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class SumScaledSourceType extends BaseSourceType {

    @Override
    public List<Metric> apply(String rawValue, List<Source> sources, OffsetDateTime instant) {
        Source sourcesFirst = sources.getFirst();
        try {
            BigDecimal sum = Arrays.stream(rawValue.split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(BigDecimal::new)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigInteger scaled = sum.multiply(new BigDecimal(sourcesFirst.scale())).toBigInteger();
            return List.of(metric(sourcesFirst, instant, scaled));
        } catch (NumberFormatException e) {
            log.warn("Could not parse sum scaled value '{}' for sourcesFirst {}", rawValue, sourcesFirst.name());
            return List.of(nullMetric(sourcesFirst, instant));
        }
    }
}
