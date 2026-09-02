package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.Metric;
import es.vodafone.sid.poller.model.Source;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.List;

public abstract class BaseSourceType implements SourceType {

    protected static Metric metric(Source source, OffsetDateTime instant, BigInteger value) {
        return new Metric(
            instant,
            source.id(), source.elementId(), source.elementTypeId(),
            source.siteId(), source.cdcId(), source.zoneId(), source.netId(),
            source.archId(), source.groupId(), source.serviceId(), source.serviceTypeId(),
            value
        );
    }

    protected static BigInteger parse(String rawValue) {
        return new BigInteger(rawValue.trim());
    }

    public static Metric nullMetric(Source source, OffsetDateTime instant) {
        return metric(source, instant, null);
    }

}
