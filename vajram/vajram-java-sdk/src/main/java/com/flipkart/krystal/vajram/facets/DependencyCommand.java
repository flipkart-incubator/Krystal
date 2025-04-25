package com.flipkart.krystal.vajram.facets;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface DependencyCommand<T> permits One2OneCommand, FanoutCommand {

  String EMPTY_STRING = "";

  List<@Nullable T> inputs();

  boolean shouldSkip();

  String doc();

  @Nullable Throwable skipCause();
}
