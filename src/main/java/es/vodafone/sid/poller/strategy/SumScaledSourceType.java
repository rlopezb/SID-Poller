package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Source;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class SumScaledSourceType extends BaseSourceType {

    @Override
    public List<Metric> calculate(String rawValue, List<Source> sources, Instant instant) {
        Source sourcesFirst = sources.getFirst();
        try {
            BigInteger sum = Arrays.stream(rawValue.split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(BigInteger::new)
                .reduce(BigInteger.ZERO, BigInteger::add);
            BigInteger scaled = sum.multiply(BigInteger.valueOf(sourcesFirst.scale()));
            return List.of(metric(sourcesFirst, instant, scaled));
        } catch (NumberFormatException e) {
            log.warn("Could not parse sum scaled value '{}' for sourcesFirst {}", rawValue, sourcesFirst.name());
            return List.of(nullMetric(sourcesFirst, instant));
        }
    }
}
