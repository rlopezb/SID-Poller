package es.vodafone.sid.poller.source;

import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Source;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;

@Slf4j
public class SumLinesSourceType extends SingleSourceType {

    @Override
    public Metric calculate(String rawValue, Source source, Instant instant, Long ticks) {
        try {
            BigInteger sum = Arrays.stream(rawValue.split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(BigInteger::new)
                .reduce(BigInteger.ZERO, BigInteger::add);
            return metric(source, instant, sum);
        } catch (NumberFormatException e) {
            log.warn("Could not parse sum lines value '{}' for source {}", rawValue, source.name());
            return nullMetric(source, instant);
        }
    }
}
