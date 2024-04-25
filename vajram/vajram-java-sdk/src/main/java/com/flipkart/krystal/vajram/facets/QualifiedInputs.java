package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.Nullable;

public record QualifiedInputs(
    int dependencyId, @Nullable DataAccessSpec spec, ImmutableSet<String> inputNames) {

  public QualifiedInputs(int dependencyId, DataAccessSpec spec, String targetInput) {
    this(dependencyId, spec, ImmutableSet.of(targetInput));
  }

  public QualifiedInputs(int dependencyId, ImmutableSet<String> inputNames) {
    this(dependencyId, null, inputNames);
  }

  public QualifiedInputs(int dependencyId, String inputName) {
    this(dependencyId, null, ImmutableSet.of(inputName));
  }
}
