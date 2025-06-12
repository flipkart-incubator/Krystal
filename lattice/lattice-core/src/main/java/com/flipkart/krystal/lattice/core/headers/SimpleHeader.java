package com.flipkart.krystal.lattice.core.headers;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

public record SimpleHeader(String name, @Nullable String value) implements Header {

  @Override
  public boolean equals(@Nullable Object o) {
    if (!(o instanceof Header that)) return false;
    return Objects.equals(name, that.name()) && Objects.equals(value, that.value());
  }
}
