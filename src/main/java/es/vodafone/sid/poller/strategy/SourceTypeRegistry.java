package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.repository.SourceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SourceTypeRegistry {

    private static final short TYPE_DIRECT = 1;
    private static final short TYPE_SUM_LINES = 2;
    private static final short TYPE_SCALED = 3;
    private static final short TYPE_SUM_SCALED = 4;
    private static final short TYPE_COUNTER = 5;
    private static final short TYPE_DIRECT_ALT = 6;
    public static final short TYPE_MULTI_CAPTURE = 7;

    private final SourceRepository sourceRepository;

    private Map<Short, SourceType> registry;

    @PostConstruct
    public void init() {
        SourceType direct = new DirectSourceType();
        registry = Map.of(
            TYPE_DIRECT, direct,
            TYPE_SUM_LINES, new SumLinesSourceType(),
            TYPE_SCALED, new ScaledSourceType(),
            TYPE_SUM_SCALED, new SumScaledSourceType(),
            TYPE_COUNTER, new CounterSourceType(sourceRepository),
            TYPE_DIRECT_ALT, direct,
            TYPE_MULTI_CAPTURE, new MultiCaptureSourceType()
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
