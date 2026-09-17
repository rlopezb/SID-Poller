package es.vodafone.sid.poller.strategy;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MultiSourceTypeRegistry extends SourceTypeRegistry {
  @PostConstruct
  public void init() {
    registry = Map.of(
        TYPE_MULTI_CAPTURE, new MultiCaptureSourceType()
    );
  }

}
