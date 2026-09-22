package es.vodafone.sid.poller.rule;

import es.vodafone.sid.poller.model.Element;
import es.vodafone.sid.poller.model.Rule;
import es.vodafone.sid.poller.model.Source;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.smi.OID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class InterfaceRuleType extends MultiRuleType {
  @Override
  public List<Source> calculate(Rule rule, Element element, Map<OID, String> results) {
    List<Source> sources = new ArrayList<>();
    String result = String.join(" ", results.values());
    if (Pattern.compile(rule.pattern()).matcher(result).find()) {
      OID oid = null;
      if (!results.isEmpty()) {
        oid = results.keySet().iterator().next();
      }
      assert oid != null;
      String index = oid.toString().substring(oid.toString().lastIndexOf('.') + 1);
      Matcher matcher = Pattern.compile(rule.name()).matcher(result);
      String name = matcher.find() ? matcher.group() : null;
      String address = rule.address().replaceAll("%INST%",index);
      Source source = new Source(
          null,
          name,
          result,
          rule.srcType(),
          element.id(),
          rule.elementTypeId(),
          element.siteId(),
          element.cdcId(),
          element.zoneId(),
          element.netId(),
          element.archId(),
          rule.grpId(),
          rule.serviceId(),
          rule.serviceTypeId(),
          rule.collectorId(),
          rule.discovererId(),
          address,
          null,
          null,
          null,
          rule.scale(),
          true,
          null
      );
      sources.add(source);
    }
    return sources;
  }
}
