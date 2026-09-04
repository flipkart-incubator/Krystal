package com.flipkart.krystal.vajram.json.array;

import com.flipkart.krystal.model.array.ByteArrayBase;
import java.util.Arrays;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectReader;

public final class JsonByteArray extends ByteArrayBase {

  private static final JsonByteArray EMPTY = new JsonByteArray(new byte[0]);

  private final byte[] data;

  private JsonByteArray(byte[] data) {
    super(data);
    this.data = data;
  }

  public static JsonByteArray empty() {
    return EMPTY;
  }

  /** Wraps a cloned copy of the provided byte array into a {@link JsonByteArray}. */
  public static JsonByteArray copyOf(byte... data) {
    return backedBy(data.clone());
  }

  /**
   * Wraps the provided byte array into a {@link JsonByteArray}. The provided array is not copied.
   * If a copy is needed, use {@link #copyOf(byte...)}.
   *
   * <p>Call this method only if you need to avoid copying the bytes for performance reasons and are
   * 100% sure that the passed bytes are not modified after passing it to this method. Otherwise,
   * use {@link #copyOf(byte...)}.
   */
  public static JsonByteArray backedBy(byte... data) {
    return new JsonByteArray(data);
  }

  @Override
  public JsonByteArray subArray(int startIndexInclusive, int endIndexExclusive) {
    return new JsonByteArray(Arrays.copyOfRange(data, startIndexInclusive, endIndexExclusive));
  }

  /** The ObjectReader must not modify the provided byte array. */
  public <T> T readFromJson(ObjectReader reader) {
    return reader.readValue(data);
  }

  /** The JsonGenerator must not modify the provided byte array. */
  public void writeBase64ToJson(JsonGenerator generator) {
    generator.writeBinary(data);
  }
}
