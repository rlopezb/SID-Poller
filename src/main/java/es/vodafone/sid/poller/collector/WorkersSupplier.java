package es.vodafone.sid.poller.collector;

import es.vodafone.sid.poller.model.SidData;

import java.util.List;
import java.util.concurrent.Callable;

@FunctionalInterface
public interface WorkersSupplier {
  List<Callable<List<SidData>>> get();
}