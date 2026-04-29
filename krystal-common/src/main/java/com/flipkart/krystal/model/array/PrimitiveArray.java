package com.flipkart.krystal.model.array;

import com.flipkart.krystal.model.Model;
import java.util.List;

/**
 * Represents an array of primitives designed to be used in {@link Model}s. Krystal modelling
 * framework doesn't allow primitive arrays to be used as is because they are mutable and can lead
 * to subtle bugs
 *
 * @param <P> the boxed version of the primitive type
 */
public sealed interface PrimitiveArray<P> permits ByteArray, DoubleArray, IntArray, LongArray {
  /** Returns an unmodifiable list view of the array. */
  List<P> asList();

  boolean isEmpty();

  int length();
}
