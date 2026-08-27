package es.vodafone.sid.poller.collector;

import es.vodafone.sid.poller.model.MetricRecord;

import java.util.List;
import java.util.concurrent.Callable;

public interface Collector extends Callable<List<MetricRecord>> {
}
