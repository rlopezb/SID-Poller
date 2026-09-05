package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.aggregator.Aggregator;
import es.vodafone.sid.poller.aggregator.SnmpAggregator;
import es.vodafone.sid.poller.aggregator.SshAggregator;
import es.vodafone.sid.poller.model.Collector;
import es.vodafone.sid.poller.model.Protocol;
import es.vodafone.sid.poller.repository.ElementRepository;
import es.vodafone.sid.poller.repository.ProtocolRepository;
import es.vodafone.sid.poller.repository.SourceRepository;
import es.vodafone.sid.poller.strategy.SourceTypeRegistry;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.client.SshClient;
import org.snmp4j.Snmp;
import org.snmp4j.smi.UdpAddress;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
public class AggregatorFactory {
  private final ElementRepository elementRepository;
  private final SourceRepository sourceRepository;
  private final ProtocolRepository protocolRepository;
  private final SshClient sshClient;
  private final Snmp snmp;
  private final BiConsumer<Protocol, UdpAddress> snmpUserRegistry;
  private final SourceTypeRegistry sourceTypeRegistry;

  public Aggregator create(Collector collector, WorkerService workerService) {
    return switch (collector.protocol().toUpperCase()) {
      case "SSH" -> new SshAggregator(collector, workerService,
          elementRepository, sourceRepository, protocolRepository,
          sshClient, sourceTypeRegistry
      );
      case "SNMP" -> new SnmpAggregator(collector, workerService,
          elementRepository, sourceRepository, protocolRepository,
          snmp, snmpUserRegistry, sourceTypeRegistry
      );
      default -> throw new IllegalArgumentException("Unknown protocol: " + collector.protocol());
    };
  }
}