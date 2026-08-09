package com.flipkart.krystal.lattice.core.headers;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.PolyNull;

public interface Header {
  String name();

  List<String> values();

  static @PolyNull Header of(String name, @PolyNull List<String> value) {
    if (value == null) {
      return null;
    }
    if (value.size() == 1) {
      return new SingleValueHeader(name, value.get(0));
    } else {
      return new HeaderImpl(name, ImmutableList.copyOf(value));
    }
  }
}
