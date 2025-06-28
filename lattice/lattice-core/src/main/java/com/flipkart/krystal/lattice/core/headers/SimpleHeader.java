package com.flipkart.krystal.lattice.core.headers;

import com.google.common.collect.ImmutableList;
import lombok.Getter;

@Getter
public final class SimpleHeader extends HeaderImpl {
  String value;

  public SimpleHeader(String name, String value) {
    super(name, ImmutableList.of(value));
    this.value = value;
  }
}
