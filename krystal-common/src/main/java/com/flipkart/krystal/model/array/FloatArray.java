package com.flipkart.krystal.model.array;

import static java.lang.Float.floatToIntBits;

import java.util.stream.Stream;

public non-sealed interface FloatArray extends PrimitiveArray<Float> {

  float valueAt(int index);

  boolean contains(float target);

  void forEach(FloatConsumer consumer);

  int indexOf(float target);

  int lastIndexOf(float target);

  Stream<Float> boxedStream();

  float[] toArray();

  FloatArray subArray(int startIndexInclusive, int endIndexExclusive);

  static boolean areEqual(float target, float b) {
    // See java.lang.Float#equals(Object)
    return floatToIntBits(b) == floatToIntBits(target);
  }

  static boolean areEqual(FloatArray a, FloatArray b) {
    if (a == b) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    int length = a.length();
    if (length != b.length()) {
      return false;
    }
    for (int i = 0; i < length; i++) {
      if (!areEqual(a.valueAt(i), b.valueAt(i))) {
        return false;
      }
    }
    return true;
  }
}
