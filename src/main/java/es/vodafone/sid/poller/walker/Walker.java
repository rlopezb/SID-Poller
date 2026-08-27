package es.vodafone.sid.poller.walker;

import es.vodafone.sid.poller.model.SourceRecord;

import java.util.List;
import java.util.concurrent.Callable;

public interface Walker extends Callable<List<SourceRecord>> {
}
