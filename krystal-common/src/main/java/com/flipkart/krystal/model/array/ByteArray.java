package com.flipkart.krystal.model.array;

public non-sealed interface ByteArray extends PrimitiveArray<Byte> {
  byte valueAt(int index);

  boolean contains(byte target);

  void forEach(ByteConsumer consumer);

  int indexOf(byte target);

  int lastIndexOf(byte target);

  byte[] toArray();

  ByteArray subArray(int startIndexInclusive, int endIndexExclusive);

  static boolean areEqual(ByteArray a, ByteArray b) {
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
      if (a.valueAt(i) != b.valueAt(i)) {
        return false;
      }
    }
    return true;
  }
}
