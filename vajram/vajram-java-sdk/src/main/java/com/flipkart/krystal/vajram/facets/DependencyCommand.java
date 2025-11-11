package com.flipkart.krystal.vajram.facets;

import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface DependencyCommand<T> permits One2OneCommand, FanoutCommand {

  String EMPTY_STRING = "";

  Collection<? extends @Nullable T> values();

  boolean shouldSkip();

  String doc();

  @Nullable Throwable skipCause();
}
