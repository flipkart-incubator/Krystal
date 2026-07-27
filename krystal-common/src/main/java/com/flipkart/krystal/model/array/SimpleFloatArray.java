package com.flipkart.krystal.model.array;

import java.util.Arrays;

/** A {@link FloatArray} that wraps a {@code float[]}. */
public final class SimpleFloatArray extends FloatArrayBase {

  private static final SimpleFloatArray EMPTY = new SimpleFloatArray(new float[0]);

  private final float[] data;

  public static SimpleFloatArray empty() {
    return EMPTY;
  }

  /** Wraps a cloned copy of the provided float array into a {@link SimpleFloatArray}. */
  public static SimpleFloatArray copyOf(float... data) {
    return backedBy(data.clone());
  }

  public static SimpleFloatArray copyOfBoxed(Float... data) {
    float[] result = new float[data.length];
    for (int i = 0; i < data.length; i++) {
      result[i] = data[i];
    }
    return SimpleFloatArray.backedBy(result);
  }

  /**
   * Wraps the provided float array into a {@link SimpleFloatArray}. The provided array is not
   * copied. If a copy is needed, use {@link #copyOf(float...)}.
   *
   * <p>Call this method only if you need to avoid copying the floats for performance reasons and
   * are 100% sure that the passed floats are not modified after passing it to this method.
   * Otherwise, use {@link #copyOf(float...)}.
   */
  public static SimpleFloatArray backedBy(float... data) {
    return new SimpleFloatArray(data);
  }

  private SimpleFloatArray(float[] data) {
    super(data);
    this.data = data;
  }

  @Override
  public FloatArray subArray(int startIndexInclusive, int endIndexExclusive) {
    return new SimpleFloatArray(Arrays.copyOfRange(data, startIndexInclusive, endIndexExclusive));
  }
}
