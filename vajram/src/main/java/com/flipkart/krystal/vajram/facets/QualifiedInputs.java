package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.Nullable;

public record QualifiedInputs(
    String dependencyName, @Nullable DataAccessSpec spec, ImmutableSet<String> inputNames) {

  public QualifiedInputs(String dependencyName, String inputName) {
    this(dependencyName, null, ImmutableSet.of(inputName));
  }
}
