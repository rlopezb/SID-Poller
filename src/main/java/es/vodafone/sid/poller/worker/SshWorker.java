package es.vodafone.sid.poller.worker;

import es.vodafone.sid.poller.model.SidData;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
public class SshWorker implements Callable<List<SidData>> {
  @Override
  public List<SidData> call() throws Exception {
    Thread.sleep(5000);
    List<SidData> results = new ArrayList<>();
    results.add(new SidData(Instant.now(), BigDecimal.ONE));
    results.add(new SidData(Instant.now(),BigDecimal.TEN));
    log.debug("SSH data collected: {}", results);
    return results;
  }
}
