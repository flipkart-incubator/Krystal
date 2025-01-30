package com.flipkart.krystal.vajram.facets;

import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface DependencyCommand<T> permits SingleExecute, MultiExecute {

  String EMPTY_STRING = "";

  ImmutableList<T> inputs();

  boolean shouldSkip();

  String doc();

  @Nullable Throwable skipCause();
}
