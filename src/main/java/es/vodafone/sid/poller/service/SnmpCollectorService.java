package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.collector.SnmpCollector;
import es.vodafone.sid.poller.config.SnmpCollectorConfiguration;
import es.vodafone.sid.poller.model.SidData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
@Slf4j
class SnmpCollectorService {
  private final ExecutorService executor;
  private final SnmpCollector snmpCollector;
  private final SnmpCollectorConfiguration snmpCollectorConfiguration;

  public SnmpCollectorService(SnmpCollector snmpCollector, SnmpCollectorConfiguration snmpCollectorConfiguration) {
    this.snmpCollector = snmpCollector;
    this.snmpCollectorConfiguration = snmpCollectorConfiguration;
    this.executor = Executors.newSingleThreadExecutor();
  }

  public void collect() {
    Future<List<SidData>> future = executor.submit(snmpCollector);
    List<SidData> result = new ArrayList<>();
    try {
      result.addAll(future.get(snmpCollectorConfiguration.collectorTimeout(), TimeUnit.MILLISECONDS));
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      future.cancel(true);
      log.error("SnmpCollectorService collect failed ({})", e.getClass().getSimpleName());
    }
    log.debug("SnmpCollectorService collect results with size: {}", result.size());
  }
}
