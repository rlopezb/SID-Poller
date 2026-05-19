package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.config.SnmpCollectorConfiguration;
import es.vodafone.sid.poller.model.SidData;
import es.vodafone.sid.poller.worker.SnmpWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
@Slf4j
public class SnmpWorkersService {
  private final ExecutorService executor;
  private final SnmpCollectorConfiguration snmpCollectorConfiguration;

  SnmpWorkersService(SnmpCollectorConfiguration snmpCollectorConfiguration) {
    this.snmpCollectorConfiguration = snmpCollectorConfiguration;
    this.executor = Executors.newFixedThreadPool(snmpCollectorConfiguration.size(), Thread.ofVirtual().factory());
  }

  public List<SidData> get(List<SnmpWorker> workers){
    List<Future<List<SidData>>> futures = new ArrayList<>();
    workers.forEach(worker -> {
      Future<List<SidData>> future = executor.submit(worker);
      futures.add(future);
    });
    List<SidData> results = new ArrayList<>();
    futures.forEach(future -> {
      try {
        results.addAll(future.get(snmpCollectorConfiguration.workerTimeout(), TimeUnit.MILLISECONDS));
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        future.cancel(true);
        log.error("SnmpWorkersService get failed ({})", e.getClass().getSimpleName());
      }
    });
    return results;
  }
}
