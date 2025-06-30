package com.flipkart.krystal.lattice.core.headers;

import com.google.common.collect.ImmutableList;
import lombok.Getter;

@Getter
public final class SingleValueHeader extends HeaderImpl {
  String value;

  public SingleValueHeader(String name, String value) {
    super(name, ImmutableList.of(value));
    this.value = value;
  }
}
