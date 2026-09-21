package es.vodafone.sid.poller.source;

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
    private static final long TICKS_MODULUS = 1L << 32; // sysUpTime wraps every ~497 days
    private static final long MICROS_PER_TICK = 10_000L; // 1 tick = 1/100 s

    private final SourceRepository sourceRepository;
    private final BigInteger wrapModulus;

    @Override
    public Metric calculate(String rawValue, Source source, Instant instant, Long ticks) {
        BigInteger current = BigInteger.valueOf(Long.parseLong(rawValue.trim()));

        if (source.instant() == null) {
            log.debug("First reading for counter source {}, storing initial value", source.name());
            sourceRepository.updateCacheInstantAndTicks(source.id(), current, instant, ticks);
            return nullMetric(source, instant);
        }

        BigInteger delta = current.subtract(source.cache());
        if (delta.compareTo(BigInteger.ZERO)< 0) {
            delta = delta.add(wrapModulus);
        }

        sourceRepository.updateCacheInstantAndTicks(source.id(), current, instant, ticks);
        long micros = elapsedMicros(source, instant, ticks);
        log.debug("Elapsed micros since last time: {}",micros);
        BigInteger rate = micros > 0
            ? delta.multiply(BigInteger.valueOf(8)).multiply(BigInteger.valueOf(1000000)).divide(BigInteger.valueOf(micros))
            : BigInteger.ZERO;

        return metric(source, instant, rate);
    }
    private long elapsedMicros(Source source, Instant instant, Long ticks) {
        if (ticks != null && source.ticks() != null) {
            long deltaTicks = ticks - source.ticks();
            if (deltaTicks < 0) {
                deltaTicks += TICKS_MODULUS; // sysUpTime is unsigned 32-bit, wraps at 2^32 centiseconds
            }
            return deltaTicks * MICROS_PER_TICK;
        }
        log.debug("No device ticks reference for source {}, falling back to poller clock", source.name());
        return ChronoUnit.MICROS.between(source.instant(), instant);
    }
}
