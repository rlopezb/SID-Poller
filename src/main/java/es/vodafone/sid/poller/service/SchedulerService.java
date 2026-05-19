package es.vodafone.sid.poller.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchedulerService {
  private final SnmpCollectorService snmpCollectorService;
  private final SshCollectorService sshCollectorService;


  @Scheduled(cron = "${sid.poller.collector.snmp.cron}")
  public void collectSnmp() {
    snmpCollectorService.collect();
  }

  @Scheduled(cron = "${sid.poller.collector.ssh.cron}")
  public void collectSsh() {
    sshCollectorService.collect();
  }
}