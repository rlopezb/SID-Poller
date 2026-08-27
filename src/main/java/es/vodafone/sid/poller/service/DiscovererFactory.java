package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.discoverer.Discoverer;
import es.vodafone.sid.poller.model.*;
import es.vodafone.sid.poller.repository.ElementRepository;
import es.vodafone.sid.poller.repository.PatternRepository;
import es.vodafone.sid.poller.repository.ProtocolRepository;
import es.vodafone.sid.poller.walker.SnmpWalker;
import es.vodafone.sid.poller.walker.SshWalker;
import es.vodafone.sid.poller.walker.Walker;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.client.SshClient;
import org.snmp4j.Snmp;
import org.snmp4j.smi.UdpAddress;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
public class DiscovererFactory {

  private final ElementRepository elementRepository;
  private final PatternRepository patternRepository;
  private final ProtocolRepository protocolRepository;
  private final SshClient sshClient;
  private final Snmp snmp;
  private final BiConsumer<ProtocolRecord, UdpAddress> snmpUserRegistry;

  public Discoverer create(DiscovererRecord discovererRecord, WalkersService walkersService) {
    return switch (discovererRecord.protocol().toUpperCase()) {
      case "SSH"  -> () -> walkSsh(discovererRecord, walkersService);
      case "SNMP" -> () -> walkSnmp(discovererRecord, walkersService);
      default -> throw new IllegalArgumentException("Unknown protocol: " + discovererRecord.protocol());
    };
  }

  private List<SourceRecord> walkSsh(DiscovererRecord discovererRecord, WalkersService walkersService) {
    List<ElementRecord> elements = elementRepository.findAll();
    Map<Short, ProtocolRecord> protocolCache = new HashMap<>();

    List<Walker> walkers = new ArrayList<>();
    for (ElementRecord element : elements) {
      List<PatternRecord> patterns = patternRepository
          .findByDiscovererAndElementTypeId(discovererRecord.protocol(), element.elementTypeId());
      if (patterns.isEmpty()) continue;

      ProtocolRecord protocol = protocolCache.computeIfAbsent(element.elementTypeId(),
          id -> protocolRepository.getByProtocolAndElementTypeId(discovererRecord.protocol(), id));

      walkers.add(new SshWalker(discovererRecord.id(), element, patterns, protocol, sshClient));
    }
    return walkersService.get(walkers);
  }

  private List<SourceRecord> walkSnmp(DiscovererRecord discovererRecord, WalkersService walkersService) {
    List<ElementRecord> elements = elementRepository.findAll();
    Map<Short, ProtocolRecord> protocolCache = new HashMap<>();

    List<Walker> walkers = new ArrayList<>();
    for (ElementRecord element : elements) {
      List<PatternRecord> patterns = patternRepository
          .findByDiscovererAndElementTypeId(discovererRecord.protocol(), element.elementTypeId());
      if (patterns.isEmpty()) continue;

      ProtocolRecord protocol = protocolCache.computeIfAbsent(element.elementTypeId(),
          id -> protocolRepository.getByProtocolAndElementTypeId(discovererRecord.protocol(), id));

      walkers.add(new SnmpWalker(discovererRecord.id(), element, patterns, protocol, snmp, snmpUserRegistry));
    }
    return walkersService.get(walkers);
  }
}