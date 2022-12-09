package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableMap;

public record Request(ImmutableMap<String, SingleResult<?>> content) {

  public Request() {
    this(ImmutableMap.of());
  }

  public ImmutableMap<String, SingleResult<?>> asMap() {
    return content;
  }
}
