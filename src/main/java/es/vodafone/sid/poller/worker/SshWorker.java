package es.vodafone.sid.poller.worker;

import es.vodafone.sid.poller.model.Metric;
import lombok.extern.slf4j.Slf4j;


import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@Slf4j
public class SshWorker implements Callable<List<Metric>> {
  private static final int METRICS_COUNT = 15;
  private static final double MIN_VALUE = 0.0;
  private static final double MAX_VALUE = 1000000.0;
  private short randomShort() {
    return (short) ThreadLocalRandom.current().nextInt(0, 100);
  }
  @Override
  public List<Metric> call() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    return IntStream.range(0, METRICS_COUNT)
        .mapToObj(_ -> new Metric(
            now,
            randomShort(),
            randomShort(),
            randomShort(),
            randomShort(),
            randomShort(),
            randomShort(),
            randomShort(),
            randomShort(),
            randomShort(),
            randomShort(),
            randomShort(),
            ThreadLocalRandom.current().nextDouble(MIN_VALUE, MAX_VALUE)
        ))
        .toList();
  }
}
