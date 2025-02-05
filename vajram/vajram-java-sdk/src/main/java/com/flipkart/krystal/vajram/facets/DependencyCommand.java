package com.flipkart.krystal.vajram.facets;

import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface DependencyCommand<T> permits One2OneCommand, FanoutCommand {

  String EMPTY_STRING = "";

  ImmutableList<@NonNull T> inputs();

  boolean shouldSkip();

  String doc();

  @Nullable Throwable skipCause();
}
