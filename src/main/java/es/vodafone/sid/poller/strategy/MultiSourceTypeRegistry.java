package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.repository.SourceRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MultiSourceTypeRegistry extends SourceTypeRegistry {
  public MultiSourceTypeRegistry(SourceRepository sourceRepository) {
    super(sourceRepository);
  }

  @PostConstruct
  public void init() {
    registry = Map.of(
        TYPE_MULTI_CAPTURE, new MultiCaptureSourceType()
    );
  }

}
