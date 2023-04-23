package com.flipkart.krystal.krystex.decorators.observability;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

class FakeClock extends Clock {
  private Instant instant;

  public FakeClock(Instant instant) {
    this.instant = instant;
  }

  void setInstant(Instant instant) {
    this.instant = instant;
  }

  @Override
  public ZoneId getZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    throw new UnsupportedOperationException("");
  }

  @Override
  public Instant instant() {
    return instant;
  }
}
