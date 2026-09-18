package es.vodafone.sid.poller.rule;

import es.vodafone.sid.poller.model.Element;
import es.vodafone.sid.poller.model.Rule;
import es.vodafone.sid.poller.model.Source;
import org.snmp4j.smi.OID;

import java.util.List;
import java.util.Map;

public non-sealed abstract class MultiRuleType extends RuleType {
  public abstract List<Source> calculate(Rule rule, Element element, Map<OID, String> results);

}
