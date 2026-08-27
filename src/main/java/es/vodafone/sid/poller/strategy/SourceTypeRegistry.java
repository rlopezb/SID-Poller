package es.vodafone.sid.poller.strategy;

import es.vodafone.sid.poller.model.SourceRecord;
import es.vodafone.sid.poller.repository.SourceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SourceTypeRegistry {

    private static final short TYPE_DIRECT = 1;
    private static final short TYPE_SUM_LINES = 2;
    private static final short TYPE_SCALED = 3;
    private static final short TYPE_SUM_SCALED = 4;
    private static final short TYPE_COUNTER32 = 5;
    private static final short TYPE_DIRECT_ALT = 6;
    private static final short TYPE_MULTI_CAPTURE = 7;
    private static final short TYPE_COUNTER64 = 8;

    private static final BigInteger WRAP_32 = BigInteger.TWO.pow(32);
    private static final BigInteger WRAP_64 = BigInteger.TWO.pow(64);

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
            TYPE_COUNTER32, new CounterSourceType(sourceRepository, WRAP_32),
            TYPE_COUNTER64, new CounterSourceType(sourceRepository, WRAP_64),
            TYPE_DIRECT_ALT, direct,
            TYPE_MULTI_CAPTURE, new MultiCaptureSourceType()
        );
    }

    public static short getMulti() {
        return TYPE_MULTI_CAPTURE;
    }

    public SourceType get(short type) {
        SourceType sourceType = registry.get(type);
        if (sourceType == null) {
            throw new IllegalArgumentException("Unknown source type: " + type);
        }
        return sourceType;
    }
}
