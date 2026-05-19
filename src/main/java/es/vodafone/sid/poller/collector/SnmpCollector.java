package es.vodafone.sid.poller.collector;

import es.vodafone.sid.poller.model.SidData;
import es.vodafone.sid.poller.service.SnmpWorkersService;
import es.vodafone.sid.poller.worker.SnmpWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnmpCollector implements Callable<List<SidData>> {
  private final SnmpWorkersService snmpWorkersService;

  @Override
  public List<SidData> call() {
    List<SnmpWorker> workers = new ArrayList<>();
    workers.add(new SnmpWorker());
    workers.add(new SnmpWorker());
    workers.add(new SnmpWorker());
    return snmpWorkersService.get(workers);
  }
}
