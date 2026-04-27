package com.flipkart.krystal.model.array;

public non-sealed interface ByteArray extends PrimitiveArray<Integer> {
  byte valueAt(int index);

  boolean contains(byte target);

  void forEach(ByteConsumer consumer);

  int indexOf(double target);

  int lastIndexOf(double target);

  byte[] toArray();

  ByteArray subArray(int startIndexInclusive, int endIndexExclusive);
}
