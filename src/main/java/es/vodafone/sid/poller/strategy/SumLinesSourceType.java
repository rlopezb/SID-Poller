package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.model.SourceRecord;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

public class SumLinesSourceType extends BaseSourceType {

    @Override
    public List<MetricRecord> apply(String rawValue, List<SourceRecord> sources, OffsetDateTime instant) {
        BigInteger sum = Arrays.stream(rawValue.split("\\n"))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .map(BigInteger::new)
            .reduce(BigInteger.ZERO, BigInteger::add);
        return List.of(metric(sources.getFirst(), instant, sum));
    }
}
