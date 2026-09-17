package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.repository.SourceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SingleSourceTypeRegistry extends SourceTypeRegistry {
    private final SourceRepository sourceRepository;

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
