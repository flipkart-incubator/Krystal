package com.flipkart.krystal.model.array;

import static com.flipkart.krystal.model.array.ByteArray.areEqual;

import com.google.common.primitives.Bytes;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A JSON-serializable implementation of {@link ByteArray} that wraps a {@code byte[]}. Bytes are
 * serialized directly as a Base64-encoded string in JSON.
 */
public final class SimpleByteArray implements ByteArray {

  private final byte[] data;

  private SimpleByteArray(byte[] data) {
    this.data = data;
  }

  public static SimpleByteArray copyOf(byte[] data) {
    return new SimpleByteArray(data.clone());
  }

  @Override
  public byte valueAt(int index) {
    return data[index];
  }

  @Override
  public boolean contains(byte target) {
    for (byte b : data) {
      if (b == target) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void forEach(ByteConsumer consumer) {
    for (byte b : data) {
      consumer.accept(b);
    }
  }

  @Override
  public int indexOf(byte target) {
    for (int i = 0; i < data.length; i++) {
      if (data[i] == target) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int lastIndexOf(byte target) {
    for (int i = data.length - 1; i >= 0; i--) {
      if (data[i] == target) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public byte[] toArray() {
    return data.clone();
  }

  @Override
  public ByteArray subArray(int startIndexInclusive, int endIndexExclusive) {
    return new SimpleByteArray(Arrays.copyOfRange(data, startIndexInclusive, endIndexExclusive));
  }

  @Override
  public List<Byte> asList() {
    return Bytes.asList(data);
  }

  @Override
  public boolean isEmpty() {
    return data.length == 0;
  }

  @Override
  public int length() {
    return data.length;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof ByteArray other)) {
      return false;
    }
    return areEqual(this, other);
  }

  public ByteArrayInputStream newInputStream() {
    return new ByteArrayInputStream(data);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }

  @Override
  public String toString() {
    return Arrays.toString(data);
  }
}
