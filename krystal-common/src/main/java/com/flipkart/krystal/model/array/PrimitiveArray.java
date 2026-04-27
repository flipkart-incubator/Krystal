package com.flipkart.krystal.model.array;

import java.util.List;

sealed interface PrimitiveArray<P> permits ByteArray, DoubleArray, IntArray, LongArray {
  List<P> asList();

  boolean isEmpty();

  int length();
}
