package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableMap;

public record Request(ImmutableMap<String, SingleResult<?>> dependencyResults) {

  public Request() {
    this(ImmutableMap.of());
  }

  public ImmutableMap<String, SingleResult<?>> asMap() {
    return dependencyResults;
  }
}
