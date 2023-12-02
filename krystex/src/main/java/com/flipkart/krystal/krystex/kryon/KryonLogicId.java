package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.model.KryonId;

public record KryonLogicId(KryonId kryonId, String value) {

  @Override
  public String toString() {
    return "l<%s>".formatted(value());
  }
}
