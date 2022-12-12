package com.flipkart.krystal.vajram;

public interface RequestBuilder<T extends VajramRequest> {
  T build();
}
