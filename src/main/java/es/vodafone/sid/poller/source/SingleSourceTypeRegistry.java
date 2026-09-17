package es.vodafone.sid.poller.source;

import es.vodafone.sid.poller.repository.SourceRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SingleSourceTypeRegistry extends SourceTypeRegistry {
    private final SourceRepository sourceRepository;

    public SingleSourceTypeRegistry(SourceRepository sourceRepository, SourceRepository sourceRepository1) {
        super(sourceRepository);
      this.sourceRepository = sourceRepository1;
    }

    @PostConstruct
    public void init() {
        SourceType direct = new DirectSourceType();
        registry = Map.of(
            TYPE_DIRECT, direct,
            TYPE_SUM_LINES, new SumLinesSourceType(),
            TYPE_SCALED, new ScaledSourceType(),
            TYPE_SUM_SCALED, new SumScaledSourceType(),
            TYPE_COUNTER32, new CounterSourceType(sourceRepository, WRAP_32),
            TYPE_COUNTER64, new CounterSourceType(sourceRepository, WRAP_64),
            TYPE_DIRECT_ALT, direct
        );
    }

}
