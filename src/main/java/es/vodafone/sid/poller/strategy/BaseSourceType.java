package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.model.MetricRecords;
import es.vodafone.sid.poller.model.SourceRecord;

import java.math.BigInteger;
import java.time.OffsetDateTime;

public abstract class BaseSourceType implements SourceType {

    protected static MetricRecord metric(SourceRecord source, OffsetDateTime instant, BigInteger value) {
        return MetricRecords.metric(source, instant, value);
    }

    protected static BigInteger parse(String rawValue) {
        return new BigInteger(rawValue.trim());
    }
}
