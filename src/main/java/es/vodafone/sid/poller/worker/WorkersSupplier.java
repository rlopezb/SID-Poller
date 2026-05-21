package es.vodafone.sid.poller.worker;

import es.vodafone.sid.poller.model.SidData;

import java.util.List;
import java.util.concurrent.Callable;

@FunctionalInterface
public interface WorkersSupplier {
  List<Callable<List<SidData>>> get();
}