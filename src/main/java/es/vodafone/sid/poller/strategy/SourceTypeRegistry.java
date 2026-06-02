package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.repository.SourceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SourceTypeRegistry {

    private final SourceRepository sourceRepository;

    private Map<Short, SourceType> registry;

    @PostConstruct
    public void init() {
        SourceType direct = new DirectSourceType();
        registry = Map.of(
            (short) 1, direct,
            (short) 2, new SumLinesSourceType(),
            (short) 3, new ScaledSourceType(),
            (short) 4, new SumScaledSourceType(),
            (short) 5, new CounterSourceType(sourceRepository),
            (short) 6, direct,
            (short) 7, new MultiCaptureSourceType()
        );
    }

    public SourceType get(short type) {
        SourceType sourceType = registry.get(type);
        if (sourceType == null) {
            throw new IllegalArgumentException("Unknown source type: " + type);
        }
        return sourceType;
    }
}
