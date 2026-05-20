package es.vodafone.sid.poller.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class SchedulerService {
  private final CollectorsService snmpCollectorsService;
  private final CollectorsService sshCollectorsService;
  private final AtomicBoolean snmpCollectorRunning = new AtomicBoolean(false);
  private final AtomicBoolean sshCollectorRunning = new AtomicBoolean(false);

  public SchedulerService(
      @Qualifier("snmpCollectorService") CollectorsService snmpCollectorsService,
      @Qualifier("sshCollectorService") CollectorsService sshCollectorsService) {
    this.snmpCollectorsService = snmpCollectorsService;
    this.sshCollectorsService = sshCollectorsService;
  }

  @Scheduled(cron = "${sid.poller.collector.snmp.cron}")
  public void collectSnmp() {
    if (!snmpCollectorRunning.compareAndSet(false, true)) {
      log.warn("SNMP collection already in progress");
      return;
    }
    try {
      snmpCollectorsService.collect();
    } finally {
      snmpCollectorRunning.set(false);
    }
  }

  @Scheduled(cron = "${sid.poller.collector.ssh.cron}")
  public void collectSsh() {
    if (!sshCollectorRunning.compareAndSet(false, true)) {
      log.warn("SSH collection already in progress");
      return;
    }
    try {
      sshCollectorsService.collect();
    } finally {
      sshCollectorRunning.set(false);
    }
  }
}