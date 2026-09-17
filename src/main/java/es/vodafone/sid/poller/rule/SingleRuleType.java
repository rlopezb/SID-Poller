package es.vodafone.sid.poller.rule;

import es.vodafone.sid.poller.model.Source;

public abstract non-sealed class SingleRuleType extends RuleType {
  public abstract Source calculate();
}
