package com.flipkart.krystal.vajram.facets;

import com.google.common.collect.ImmutableCollection;
import java.util.Optional;

public sealed interface DependencyCommand<T> permits SingleExecute, MultiExecute {

  String EMPTY_STRING = "";

  ImmutableCollection<Optional<T>> inputs();

  boolean shouldSkip();

  String doc();
}
