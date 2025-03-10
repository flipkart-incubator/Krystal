package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;

public record KryonLogicId(VajramID vajramID, String value) {

  @Override
  public String toString() {
    return "l<%s>".formatted(value());
  }
}
