package es.vodafone.sid.poller.rule;

import es.vodafone.sid.poller.model.Source;

import java.util.List;

public non-sealed abstract class MultiRuleType extends RuleType {
  public abstract List<Source> calculate();

}
