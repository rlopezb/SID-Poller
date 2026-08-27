package es.vodafone.sid.poller.worker;

import es.vodafone.sid.poller.model.MetricRecord;

import java.util.List;
import java.util.concurrent.Callable;

public interface Worker extends Callable<List<MetricRecord>> {
}
