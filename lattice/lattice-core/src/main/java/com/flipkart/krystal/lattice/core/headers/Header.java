package com.flipkart.krystal.lattice.core.headers;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface Header {
  String name();

  @Nullable String value();
}
