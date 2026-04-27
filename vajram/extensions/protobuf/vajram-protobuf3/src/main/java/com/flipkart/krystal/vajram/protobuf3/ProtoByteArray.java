package com.flipkart.krystal.vajram.protobuf3;

import com.flipkart.krystal.model.array.ByteArray;
import com.flipkart.krystal.model.array.ByteConsumer;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A Protobuf-compatible implementation of {@link ByteArray} that wraps a {@link ByteString}. This
 * allows models to use the {@link ByteArray} interface while leveraging protobuf's native byte
 * string handling for serialization.
 */
public final class ProtoByteArray implements ByteArray {

  private final ByteString byteString;

  public ProtoByteArray(ByteString byteString) {
    this.byteString = byteString;
  }

  public ProtoByteArray(byte[] data) {
    this.byteString = ByteString.copyFrom(data);
  }

  public ByteString toByteString() {
    return byteString;
  }

  /**
   * Converts any {@link ByteArray} to a {@link ByteString}. If the input is already a {@link
   * ProtoByteArray}, extracts the wrapped ByteString directly. Otherwise, copies bytes one by one.
   */
  public static ByteString toByteString(ByteArray byteArray) {
    if (byteArray instanceof ProtoByteArray pba) {
      return pba.toByteString();
    }
    byte[] bytes = new byte[byteArray.length()];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = byteArray.valueAt(i);
    }
    return ByteString.copyFrom(bytes);
  }

  @Override
  public byte valueAt(int index) {
    return byteString.byteAt(index);
  }

  @Override
  public boolean contains(byte target) {
    for (int i = 0; i < byteString.size(); i++) {
      if (byteString.byteAt(i) == target) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void forEach(ByteConsumer consumer) {
    for (int i = 0; i < byteString.size(); i++) {
      consumer.accept(byteString.byteAt(i));
    }
  }

  @Override
  public int indexOf(double target) {
    byte b = (byte) target;
    for (int i = 0; i < byteString.size(); i++) {
      if (byteString.byteAt(i) == b) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int lastIndexOf(double target) {
    byte b = (byte) target;
    for (int i = byteString.size() - 1; i >= 0; i--) {
      if (byteString.byteAt(i) == b) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public byte[] toArray() {
    return byteString.toByteArray();
  }

  @Override
  public ByteArray subArray(int startIndexInclusive, int endIndexExclusive) {
    return new ProtoByteArray(byteString.substring(startIndexInclusive, endIndexExclusive));
  }

  @Override
  public List<Integer> asList() {
    List<Integer> list = new ArrayList<>(byteString.size());
    for (int i = 0; i < byteString.size(); i++) {
      list.add((int) byteString.byteAt(i));
    }
    return list;
  }

  @Override
  public boolean isEmpty() {
    return byteString.isEmpty();
  }

  @Override
  public int length() {
    return byteString.size();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return obj instanceof ProtoByteArray other && this.byteString.equals(other.byteString);
  }

  @Override
  public int hashCode() {
    return byteString.hashCode();
  }

  @Override
  public String toString() {
    return Arrays.toString(byteString.toByteArray());
  }
}
