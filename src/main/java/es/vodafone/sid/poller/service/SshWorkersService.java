package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.config.SshCollectorConfiguration;
import es.vodafone.sid.poller.model.SidData;
import es.vodafone.sid.poller.worker.SshWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
@Slf4j
public class SshWorkersService {
  private final ExecutorService executor;
  private final SshCollectorConfiguration sshCollectorConfiguration;

  SshWorkersService(SshCollectorConfiguration sshCollectorConfiguration) {
    this.sshCollectorConfiguration = sshCollectorConfiguration;
    this.executor = new ThreadPoolExecutor(
        sshCollectorConfiguration.size(),
        sshCollectorConfiguration.size(),
        sshCollectorConfiguration.workerTimeout(),
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(sshCollectorConfiguration.size()),
        Thread.ofVirtual().factory(),
        new ThreadPoolExecutor.CallerRunsPolicy());
  }

  public List<SidData> get(List<SshWorker> workers){
    List<Future<List<SidData>>> futures = new ArrayList<>();
    workers.forEach(worker -> {
      Future<List<SidData>> future = executor.submit(worker);
      futures.add(future);
    });
    List<SidData> results = new ArrayList<>();
    futures.forEach(future -> {
      List<SidData> result = new ArrayList<>();
      try {
        result = new ArrayList<>(future.get(sshCollectorConfiguration.workerTimeout(), TimeUnit.MILLISECONDS));
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        future.cancel(true);
        log.error("SshWorkersService get failed ({})", e.getClass().getSimpleName());
      }
      results.addAll(result);
    });
    return results;
  }
}
