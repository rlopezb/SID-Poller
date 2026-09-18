package es.vodafone.sid.poller.source;

import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Source;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;

@Slf4j
public class ScaledSourceType extends SingleSourceType {

    @Override
    public Metric calculate(String rawValue, Source source, Instant instant, Long ticks) {
        try {
            BigInteger scaled = new BigDecimal(rawValue.trim())
                .multiply(new BigDecimal(source.scale()))
                .toBigInteger();
            return metric(source, instant, scaled);
        } catch (NumberFormatException e) {
            log.warn("Could not parse value '{}' for source {}", rawValue, source.name());
            return nullMetric(source, instant);
        }
    }
}
