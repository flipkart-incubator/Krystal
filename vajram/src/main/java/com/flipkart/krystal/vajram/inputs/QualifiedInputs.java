package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.Nullable;

public record QualifiedInputs(
    String dependencyName, @Nullable DataAccessSpec spec, ImmutableSet<String> inputNames) {

  public QualifiedInputs(String dependencyName, DataAccessSpec spec, String targetInputName) {
    this(dependencyName, spec, ImmutableSet.of(targetInputName));
  }
}
