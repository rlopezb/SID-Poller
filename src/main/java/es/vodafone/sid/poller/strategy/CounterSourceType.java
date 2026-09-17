package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Source;
import es.vodafone.sid.poller.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@RequiredArgsConstructor
public class CounterSourceType extends SingleSourceType {
    private final SourceRepository sourceRepository;
    private final BigInteger wrapModulus;

    @Override
    public Metric calculate(String rawValue, Source source, Instant instant) {
        BigInteger current = BigInteger.valueOf(Long.parseLong(rawValue.trim()));

        if (source.instant() == null) {
            log.debug("First reading for counter source {}, storing initial value", source.name());
            sourceRepository.updateCacheAndInstant(source.id(), current, instant);
            return nullMetric(source, instant);
        }

        long seconds = ChronoUnit.SECONDS.between(source.instant(), instant);
        BigInteger delta = current.subtract(source.cache());

        if (delta.compareTo(BigInteger.ZERO)< 0) {
            delta = delta.add(wrapModulus);
        }

        sourceRepository.updateCacheAndInstant(source.id(), current, instant);

        BigInteger rate = seconds > 0
            ? delta.divide(BigInteger.valueOf(seconds)).multiply(new BigInteger("8"))
            : BigInteger.ZERO;

        return metric(source, instant, rate);
    }
}
