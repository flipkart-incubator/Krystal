package com.flipkart.krystal.lattice.core.headers;

import java.util.List;

public record SingleValueHeader(String name, String value) implements Header {

  @Override
  public List<String> values() {
    return List.of(value);
  }
}
