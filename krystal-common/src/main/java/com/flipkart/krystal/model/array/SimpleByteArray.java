package com.flipkart.krystal.model.array;

import java.util.Arrays;

/**
 * A JSON-serializable implementation of {@link ByteArray} that wraps a {@code byte[]}. Bytes are
 * serialized directly as a Base64-encoded string in JSON.
 */
public final class SimpleByteArray extends ByteArrayBase {

  private static final SimpleByteArray EMPTY = new SimpleByteArray(new byte[0]);
  private final byte[] data;

  private SimpleByteArray(byte[] data) {
    super(data);
    this.data = data;
  }

  public static SimpleByteArray of() {
    return EMPTY;
  }

  /** Wraps a cloned copy of the provided byte array into a {@link SimpleByteArray}. */
  public static SimpleByteArray copyOf(byte... data) {
    return of(data.clone());
  }

  /**
   * Wraps the provided byte array into a {@link SimpleByteArray}. The provided array is not copied.
   * If a copy is needed, use {@link #copyOf(byte...)}.
   *
   * <p>Call this method only if you need to avoid copying the bytes for performance reasons and are
   * 100% sure that the passed bytes are not modified after passing it to this method. Otherwise,
   * use {@link #copyOf(byte...)}.
   */
  public static SimpleByteArray of(byte... data) {
    return new SimpleByteArray(data);
  }

  @Override
  public ByteArray subArray(int startIndexInclusive, int endIndexExclusive) {
    return new SimpleByteArray(Arrays.copyOfRange(data, startIndexInclusive, endIndexExclusive));
  }
}
