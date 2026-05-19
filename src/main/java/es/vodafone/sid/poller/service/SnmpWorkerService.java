package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.config.SnmpCollectorConfiguration;
import es.vodafone.sid.poller.config.SnmpWorkerConfiguration;
import es.vodafone.sid.poller.model.SidData;
import es.vodafone.sid.poller.worker.SnmpWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
class SnmpWorkerService {
  private final ExecutorService executor;
  private final SnmpCollectorConfiguration snmpCollectorConfiguration;
  private final SnmpWorkerConfiguration snmpWorkerConfiguration;

  SnmpWorkerService(SnmpCollectorConfiguration snmpCollectorConfiguration, SnmpWorkerConfiguration snmpWorkerConfiguration) {
    this.snmpWorkerConfiguration = snmpWorkerConfiguration;
    this.snmpCollectorConfiguration = snmpCollectorConfiguration;
    this.executor = new ThreadPoolExecutor(
        snmpCollectorConfiguration.size(),
        snmpCollectorConfiguration.size(),
        snmpWorkerConfiguration.timeout(),
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(snmpCollectorConfiguration.size()),
        Thread.ofVirtual().factory(),
        new ThreadPoolExecutor.CallerRunsPolicy());
  }

  public List<SidData> get(List<SnmpWorker> workers){
    List<SidData> result = null;

    return  result;
  }
}
