package com.flipkart.krystal.data;

@SuppressWarnings("ClassReferencesSubclass") // By Design
public sealed interface Model permits Facets, ImmutableModel, ModelBuilder {
  ModelBuilder _asBuilder();

  ImmutableModel _build();

  Model _newCopy();
}
