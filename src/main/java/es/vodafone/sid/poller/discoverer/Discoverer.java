package es.vodafone.sid.poller.discoverer;

import es.vodafone.sid.poller.model.SourceRecord;

import java.util.List;
import java.util.concurrent.Callable;

public interface Discoverer extends Callable<List<SourceRecord>> {
}
