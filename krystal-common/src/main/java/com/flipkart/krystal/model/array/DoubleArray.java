package com.flipkart.krystal.model.array;

import java.util.function.DoubleConsumer;
import java.util.stream.DoubleStream;

public non-sealed interface DoubleArray extends PrimitiveArray<Double> {
  double valueAt(int index);

  boolean contains(double target);

  void forEach(DoubleConsumer consumer);

  int indexOf(double target);

  int lastIndexOf(double target);

  DoubleStream stream();

  double[] toArray();

  DoubleArray subArray(int startIndexInclusive, int endIndexExclusive);
}
