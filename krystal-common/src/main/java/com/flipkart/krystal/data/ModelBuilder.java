package com.flipkart.krystal.data;

public non-sealed interface ModelBuilder extends Model {
  ImmutableModel _build();

  ModelBuilder _asBuilder();

  ModelBuilder _newCopy();
}
