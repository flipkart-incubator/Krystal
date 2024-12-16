package com.flipkart.krystal.vajram.facets;

import com.google.common.collect.ImmutableList;

public sealed interface DependencyCommand<T> permits SingleExecute, MultiExecute {

  String EMPTY_STRING = "";

  ImmutableList<T> inputs();

  boolean shouldSkip();

  String doc();
}
