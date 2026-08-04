package com.flipkart.krystal.core;

import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

// @jdk.internal.ValueBased
public final class VajramID {

  // Must use ConcurrentHashMap for thread safety - else we will encounter
  // ConcurrentModificationException
  private static final ConcurrentHashMap<String, VajramID> internPool = new ConcurrentHashMap<>();

  @Getter private final String id;

  private VajramID(String id) {
    this.id = id;
  }

  public static VajramID vajramID(String id) {
    return internPool.computeIfAbsent(id, _k -> new VajramID(id));
  }

  @Override
  public String toString() {
    return "v<%s>".formatted(id());
  }
}
