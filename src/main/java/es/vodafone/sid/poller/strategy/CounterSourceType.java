package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.model.SourceRecord;
import es.vodafone.sid.poller.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CounterSourceType extends BaseSourceType {

    private final SourceRepository sourceRepository;

    @Override
    public List<MetricRecord> apply(String rawValue, List<SourceRecord> sources, OffsetDateTime instant) {
        SourceRecord source = sources.getFirst();
        BigInteger current = new BigInteger(rawValue.trim());

        if (source.instant() == null) {
            log.debug("First reading for counter source {}, storing initial value", source.name());
            sourceRepository.updateCacheAndInstant(source.id(), current.doubleValue(), instant);
            return List.of();
        }

        long seconds = ChronoUnit.SECONDS.between(source.instant(), instant);
        BigInteger previous = BigDecimal.valueOf(source.cache()).toBigInteger();
        BigInteger delta = current.subtract(previous);

        // Manejar wrap-around Counter32
        if (delta.compareTo(BigInteger.ZERO) < 0) {
            delta = delta.add(BigInteger.TWO.pow(32));
        }

        sourceRepository.updateCacheAndInstant(source.id(), current.doubleValue(), instant);

        BigInteger rate = seconds > 0
            ? delta.divide(BigInteger.valueOf(seconds))
            : BigInteger.ZERO;

        return List.of(metric(source, instant, rate));
    }
}
