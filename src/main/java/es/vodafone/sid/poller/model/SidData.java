package es.vodafone.sid.poller.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
public class SidData {
  private Instant instant;
  private BigDecimal value;
}
