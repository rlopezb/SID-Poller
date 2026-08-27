package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.model.SourceRecord;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class SumScaledSourceType extends BaseSourceType {

    @Override
    public List<MetricRecord> apply(String rawValue, List<SourceRecord> sourceRecords, OffsetDateTime instant) {
        SourceRecord sourceRecordsFirst = sourceRecords.getFirst();
        try {
            BigDecimal sum = Arrays.stream(rawValue.split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(BigDecimal::new)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigInteger scaled = sum.multiply(new BigDecimal(sourceRecordsFirst.scale())).toBigInteger();
            return List.of(metric(sourceRecordsFirst, instant, scaled));
        } catch (NumberFormatException e) {
            log.warn("Could not parse sum scaled value '{}' for sourceRecordsFirst {}", rawValue, sourceRecordsFirst.name());
            return List.of(nullMetric(sourceRecordsFirst, instant));
        }
    }
}
