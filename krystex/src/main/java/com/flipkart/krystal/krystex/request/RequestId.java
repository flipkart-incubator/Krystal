package com.flipkart.krystal.krystex.request;

import java.util.Objects;

public record RequestId(Object id, RequestId originatedFrom) {

  public RequestId(Object content) {
    this(content, null);
  }

  @Override
  public RequestId originatedFrom() {
    if (originatedFrom == null) {
      return this;
    }
    return originatedFrom;
  }

  @Override
  public String toString() {
    return String.valueOf(id);
  }

  @Override
  public boolean equals(Object o) {
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
