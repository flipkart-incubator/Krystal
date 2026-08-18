package com.flipkart.krystal.vajram.facets;


import static com.google.common.collect.Collections2.transform;

import com.flipkart.krystal.data.Errable;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface DependencyCommand<T> permits One2OneCommand, FanoutCommand {

  String EMPTY_STRING = "";

  Collection<@NonNull Errable<? extends T>> errables();

  default Collection<? extends @Nullable T> values() {
    return transform(errables(), Errable::value);
  }

  boolean shouldSkip();

  String doc();

  @Nullable Throwable skipCause();
}
