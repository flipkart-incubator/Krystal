package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Request;
import org.checkerframework.checker.nullness.qual.Nullable;

@FunctionalInterface
public interface RequestValueSetter<T> {
  void setValue(Request facets, @Nullable T value);
}
