package es.vodafone.sid.poller.rule;

import es.vodafone.sid.poller.model.Element;
import es.vodafone.sid.poller.model.Rule;
import es.vodafone.sid.poller.model.Source;

public abstract sealed class RuleType permits SingleRuleType, MultiRuleType {
  protected static Source source(Rule rule, Element element) {
    return new Source(
        null,
        rule.name(),
        null,
        rule.srcType(),
        element.id(),
        rule.srcType(),
        element.siteId(),
        element.cdcId(),
        element.zoneId(),
        element.netId(),
        element.archId(),
        rule.grpId(),
        rule.serviceId(),
        rule.serviceTypeId(),
        rule.collectorId(),
        null,
        rule.address(),
        null,
        null,
        null,
        rule.scale(),
        true,
        null
    );
  }

}
