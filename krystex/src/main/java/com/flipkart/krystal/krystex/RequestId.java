package com.flipkart.krystal.krystex;

public record RequestId(String asString, RequestId originatedFrom) {

  public RequestId(String requestId) {
    this(requestId, null);
  }

  @Override
  public RequestId originatedFrom() {
    if (originatedFrom == null) {
      return this;
    }
    return originatedFrom;
  }

  public RequestId createNewRequest(Object suffix) {
    return new RequestId("%s:%s".formatted(asString, suffix), originatedFrom());
  }
}
