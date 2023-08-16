package com.flipkart.krystal.krystex.request;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

public record RequestId(Object id) {

  @Override
  public String toString() {
    return String.valueOf(id);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) return true;
    if (!(o instanceof RequestId requestId)) {
      return false;
    }
    return Objects.equals(id, requestId.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
