package es.vodafone.sid.poller.collector;

import es.vodafone.sid.poller.model.SidData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@Component
public class SshCollector implements Callable<List<SidData>> {
  @Override
  public List<SidData> call() throws Exception {
    Thread.sleep(15000);
    List<SidData> results = new ArrayList<>();
    results.add(new SidData(Instant.now(), BigDecimal.TEN));
    results.add(new SidData(Instant.now(), BigDecimal.ZERO));
    results.add(new SidData(Instant.now(), null));
    log.debug("SSH data collected: {}", results);
    return results;
  }
}
