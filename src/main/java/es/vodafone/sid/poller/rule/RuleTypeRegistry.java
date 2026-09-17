package es.vodafone.sid.poller.rule;

import es.vodafone.sid.poller.source.DirectSourceType;
import es.vodafone.sid.poller.source.SourceType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RuleTypeRegistry {
  public static final short TYPE_INTERFACE = 1;
  protected Map<Short, RuleType> registry;

  @PostConstruct
  void init() {
    SourceType direct = new DirectSourceType();
    registry = Map.of(
        TYPE_INTERFACE, new InterfaceRuleType()
    );
  }

  public RuleType get(short type) {
    RuleType ruleType = registry.get(type);
    if (ruleType == null) {
      throw new IllegalArgumentException("Unknown rule type: " + type);
    }
    return ruleType;
  }
}
