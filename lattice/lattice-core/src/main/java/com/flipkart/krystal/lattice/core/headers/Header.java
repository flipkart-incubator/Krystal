package com.flipkart.krystal.lattice.core.headers;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface Header {
  String key();

  @Nullable String value();
}
