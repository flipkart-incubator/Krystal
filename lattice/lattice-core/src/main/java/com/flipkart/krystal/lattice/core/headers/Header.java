package com.flipkart.krystal.lattice.core.headers;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Header {
  String name();

  List<String> values();

  static @Nullable Header of(String name, @Nullable List<String> value) {
    if (value == null) {
      return null;
    }
    if (value.size() == 1) {
      return new SingleValueHeader(name, value.get(0));
    } else {
      return new HeaderImpl(name, value);
    }
  }
}
