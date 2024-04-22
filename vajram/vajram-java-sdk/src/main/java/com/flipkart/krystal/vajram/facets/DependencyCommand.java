package com.flipkart.krystal.vajram.facets;

import com.google.common.collect.ImmutableList;
import java.util.Optional;

public sealed interface DependencyCommand<T> permits SingleExecute, MultiExecute {

  String EMPTY_STRING = "";

  ImmutableList<Optional<T>> inputs();

  boolean shouldSkip();

  String doc();
}
