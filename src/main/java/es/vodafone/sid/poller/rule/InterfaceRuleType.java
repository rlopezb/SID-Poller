package es.vodafone.sid.poller.rule;

import es.vodafone.sid.poller.model.Source;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class InterfaceRuleType extends SingleRuleType {
  @Override
  public Source calculate() {
    return null;
  }
}
