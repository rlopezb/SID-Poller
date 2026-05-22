package es.vodafone.sid.poller.worker;

import es.vodafone.sid.poller.model.SidData;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
public class SnmpWorker implements Callable<List<SidData>> {
  private static final int MAX_DELAY_MS = 5000;

  @Override
  public List<SidData> call(){
    long delay = (long) (Math.random() * MAX_DELAY_MS);
    log.debug("SNMP worker sleeping for {} ms", delay);
    try {
      Thread.sleep(delay);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("SNMP worker interrupted during sleep, returning empty result");
      return List.of();
    }

    List<SidData> results = new ArrayList<>();
    results.add(new SidData(Instant.now(), BigDecimal.ONE));
    results.add(new SidData(Instant.now(),BigDecimal.TEN));
    return results;
  }
}
