package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableMap;

public record Request(ImmutableMap<String, SingleResultFuture<?>> content) {

  public Request() {
    this(ImmutableMap.of());
  }

  public ImmutableMap<String, SingleResultFuture<?>> asMap() {
    return content;
  }
}
