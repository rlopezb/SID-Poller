package es.vodafone.sid.poller.finder;

import es.vodafone.sid.poller.model.Source;

import java.util.List;
import java.util.concurrent.Callable;

public interface Finder extends Callable<List<Source>> {
}
