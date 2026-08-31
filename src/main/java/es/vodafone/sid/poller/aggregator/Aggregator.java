package es.vodafone.sid.poller.aggregator;

import es.vodafone.sid.poller.model.Metric;

import java.util.List;
import java.util.concurrent.Callable;

public interface Aggregator extends Callable<List<Metric>> {
}
