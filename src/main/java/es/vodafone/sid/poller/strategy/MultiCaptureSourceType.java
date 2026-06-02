package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.MetricRecord;
import es.vodafone.sid.poller.model.SourceRecord;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class MultiCaptureSourceType extends BaseSourceType {

    @Override
    public List<MetricRecord> apply(String rawValue, List<SourceRecord> sources, OffsetDateTime instant) {
        List<MetricRecord> metrics = new ArrayList<>();
        for (SourceRecord source : sources) {
            Pattern pattern = Pattern.compile(source.capture());
            Matcher matcher = pattern.matcher(rawValue);
            if (matcher.find()) {
                metrics.add(metric(source, instant, new BigInteger(matcher.group(1))));
            } else {
                log.warn("Capture pattern '{}' did not match for source {}", source.capture(), source.name());
            }
        }
        return metrics;
    }
}
