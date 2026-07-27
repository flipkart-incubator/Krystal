package com.flipkart.krystal.model.array;

import static com.flipkart.krystal.model.array.FloatArray.areEqual;
import static java.util.Collections.unmodifiableList;

import com.google.common.primitives.Floats;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A {@link FloatArray} that wraps a {@code float[]}. */
public abstract class FloatArrayBase implements FloatArray {

  private final float[] data;

  protected FloatArrayBase(float[] data) {
    this.data = data;
  }

  @Override
  public float valueAt(int index) {
    return data[index];
  }

  @Override
  public boolean contains(float target) {
    for (float b : data) {
      if (areEqual(b, target)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void forEach(FloatConsumer consumer) {
    for (float b : data) {
      consumer.accept(b);
    }
  }

  @Override
  public int indexOf(float target) {
    for (int i = 0; i < data.length; i++) {
      if (areEqual(data[i], target)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int lastIndexOf(float target) {
    for (int i = data.length - 1; i >= 0; i--) {
      if (areEqual(data[i], target)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public Stream<Float> boxedStream() {
    return asList().stream();
  }

  @Override
  public float[] toArray() {
    return data.clone();
  }

  @Override
  public List<Float> asList() {
    return unmodifiableList(Floats.asList(data));
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
    if (!(obj instanceof FloatArray other)) {
      return false;
    }
    if (other instanceof FloatArrayBase floatArrayBase) {
      return Arrays.equals(data, floatArrayBase.data);
    }
    return areEqual(this, other);
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
