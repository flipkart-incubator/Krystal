package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Request;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface RequestValueGetter<T> {
  @Nullable T getValue(Request facets);
}
