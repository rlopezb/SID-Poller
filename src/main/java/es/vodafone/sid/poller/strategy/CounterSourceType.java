package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Source;
import es.vodafone.sid.poller.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CounterSourceType extends BaseSourceType {
    private final SourceRepository sourceRepository;
    private final BigInteger wrapModulus;

    @Override
    public List<Metric> calculate(String rawValue, List<Source> sources, OffsetDateTime instant) {
        Source source = sources.getFirst();
        BigInteger current = new BigInteger(rawValue.trim());

        if (source.instant() == null) {
            log.debug("First reading for counter source {}, storing initial value", source.name());
            sourceRepository.updateCacheAndInstant(source.id(), current, instant);
            return List.of(BaseSourceType.nullMetric(source, instant));
        }

        long seconds = ChronoUnit.SECONDS.between(source.instant(), instant);
        BigInteger delta = current.subtract(source.cache());

        if (delta.compareTo(BigInteger.ZERO) < 0) {
            delta = delta.add(wrapModulus);
        }

        sourceRepository.updateCacheAndInstant(source.id(), current, instant);

        BigInteger rate = seconds > 0
            ? delta.divide(BigInteger.valueOf(seconds))
            : BigInteger.ZERO;

        return List.of(metric(source, instant, rate));
    }
}
