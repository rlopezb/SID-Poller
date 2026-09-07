package es.vodafone.sid.poller.walker;

import es.vodafone.sid.poller.model.Element;
import es.vodafone.sid.poller.model.Protocol;
import es.vodafone.sid.poller.model.Rule;
import es.vodafone.sid.poller.model.Source;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.concurrent.Callable;

@RequiredArgsConstructor
public abstract class Walker implements Callable<List<Source>> {
  public final short discovererId;
  public final Element element;
  public final List<Rule> rules;
  public final Protocol protocol;
}
