package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableMap;

public record Request(ImmutableMap<String, Result<?>> dependencyResults) {

  public ImmutableMap<String, Result<?>> asMap() {
    return dependencyResults;
  }
}
