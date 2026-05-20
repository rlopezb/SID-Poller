package es.vodafone.sid.poller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {
  private final CollectorService snmpCollectorService;
  private final CollectorService sshCollectorService;
  private final AtomicBoolean snmpCollectorRunning = new AtomicBoolean(false);
  private final AtomicBoolean sshCollectorRunning = new AtomicBoolean(false);

  @Scheduled(cron = "${sid.poller.collector.snmp.cron}")
  public void collectSnmp() {
    if (!snmpCollectorRunning.compareAndSet(false, true)) {
      log.warn("SNMP collection already in progress");
      return;
    }
    try {
      snmpCollectorService.collect();
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
      sshCollectorService.collect();
    } finally {
      sshCollectorRunning.set(false);
    }
  }
}