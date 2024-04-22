package com.flipkart.krystal.data;

public interface ModelBuilder {
  ImmutableModel _build();

  ModelBuilder _newCopy();
}
