package com.flipkart.krystal.lattice.core.headers;

import java.util.List;

public interface Header {
  String name();

  List<String> values();

  static Header of(String name, List<String> value) {
    if (value.size() == 1) {
      return new SingleValueHeader(name, value.get(0));
    } else {
      return new HeaderImpl(name, value);
    }
  }
}
