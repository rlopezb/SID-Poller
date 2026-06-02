package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.model.SourceRecord;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.List;

public class ScaledSourceType extends BaseSourceType {

    @Override
    public List<MetricRecord> apply(String rawValue, List<SourceRecord> sources, OffsetDateTime instant) {
        SourceRecord source = sources.getFirst();
        BigInteger scaled = new BigDecimal(rawValue.trim())
            .multiply(BigDecimal.valueOf(source.cache()))
            .toBigInteger();
        return List.of(metric(source, instant, scaled));
    }
}
