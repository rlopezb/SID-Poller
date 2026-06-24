package es.vodafone.sid.poller.service;

import es.vodafone.sid.poller.model.*;
import es.vodafone.sid.poller.repository.ElementRepository;
import es.vodafone.sid.poller.repository.PatternRepository;
import es.vodafone.sid.poller.repository.ProtocolRepository;
import es.vodafone.sid.poller.walker.SnmpWalker;
import es.vodafone.sid.poller.walker.SshWalker;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.client.SshClient;
import org.snmp4j.Snmp;
import org.snmp4j.smi.UdpAddress;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class DiscovererFactory {

  private final ElementRepository elementRepository;
  private final PatternRepository patternRepository;
  private final ProtocolRepository protocolRepository;
  private final SshClient sshClient;
  private final Snmp snmp;
  private final BiConsumer<ProtocolRecord, UdpAddress> snmpUserRegistry;

  public Callable<List<SourceRecord>> create(DiscovererRecord discoverer, WalkersService walkersService) {
    return switch (discoverer.protocol().toUpperCase()) {
      case "SSH"  -> () -> walkSsh(discoverer, walkersService);
      case "SNMP" -> () -> walkSnmp(discoverer, walkersService);
      default -> throw new IllegalArgumentException("Unknown protocol: " + discoverer.protocol());
    };
  }

  private List<SourceRecord> walkSsh(DiscovererRecord discoverer, WalkersService walkersService) {
    List<ElementRecord> elements = elementRepository.findAll();
    Map<Short, ProtocolRecord> protocolCache = new HashMap<>();

    List<Callable<List<SourceRecord>>> walkers = new ArrayList<>();
    for (ElementRecord element : elements) {
      List<PatternRecord> patterns = patternRepository
          .findByDiscovererAndElementTypeId(discoverer.protocol(), element.elementTypeId());
      if (patterns.isEmpty()) continue;

      ProtocolRecord protocol = protocolCache.computeIfAbsent(element.elementTypeId(),
          id -> protocolRepository.getByProtocolAndElementTypeId(discoverer.protocol(), id));

      walkers.add(new SshWalker(discoverer.id(), element, patterns, protocol, sshClient));
    }
    return walkersService.get(walkers);
  }

  private List<SourceRecord> walkSnmp(DiscovererRecord discoverer, WalkersService walkersService) {
    List<ElementRecord> elements = elementRepository.findAll();
    Map<Short, ProtocolRecord> protocolCache = new HashMap<>();

    List<Callable<List<SourceRecord>>> walkers = new ArrayList<>();
    for (ElementRecord element : elements) {
      List<PatternRecord> patterns = patternRepository
          .findByDiscovererAndElementTypeId(discoverer.protocol(), element.elementTypeId());
      if (patterns.isEmpty()) continue;

      ProtocolRecord protocol = protocolCache.computeIfAbsent(element.elementTypeId(),
          id -> protocolRepository.getByProtocolAndElementTypeId(discoverer.protocol(), id));

      walkers.add(new SnmpWalker(discoverer.id(), element, patterns, protocol, snmp, snmpUserRegistry));
    }
    return walkersService.get(walkers);
  }
}