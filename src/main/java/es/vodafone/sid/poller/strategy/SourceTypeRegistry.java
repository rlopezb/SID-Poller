package es.vodafone.sid.poller.strategy;

import java.math.BigInteger;
import java.util.Map;

public abstract class SourceTypeRegistry {
  protected static final short TYPE_DIRECT = 1;
  protected static final short TYPE_SUM_LINES = 2;
  protected static final short TYPE_SCALED = 3;
  protected static final short TYPE_SUM_SCALED = 4;
  protected static final short TYPE_COUNTER32 = 5;
  protected static final short TYPE_DIRECT_ALT = 6;
  protected static final short TYPE_MULTI_CAPTURE = 7;
  protected static final short TYPE_COUNTER64 = 8;

  protected static final BigInteger WRAP_32 = BigInteger.TWO.pow(32);
  protected static final BigInteger WRAP_64 = BigInteger.TWO.pow(64);
  protected Map<Short, SourceType> registry;

  public static Boolean isMulti(short type) {
    return type == TYPE_MULTI_CAPTURE;
  }

  public SourceType get(short type) {
    SourceType sourceType = registry.get(type);
    if (sourceType == null) {
      throw new IllegalArgumentException("Unknown source type: " + type);
    }
    return sourceType;
  }
}
