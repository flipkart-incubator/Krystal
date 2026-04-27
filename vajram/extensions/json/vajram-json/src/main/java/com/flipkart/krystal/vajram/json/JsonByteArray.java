package com.flipkart.krystal.vajram.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.flipkart.krystal.model.array.ByteArray;
import com.flipkart.krystal.model.array.ByteConsumer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A JSON-serializable implementation of {@link ByteArray} that wraps a {@code byte[]}. Bytes are
 * serialized directly as a Base64-encoded string in JSON.
 */
@JsonSerialize(using = JsonByteArray.Serializer.class)
@JsonDeserialize(using = JsonByteArray.Deserializer.class)
public final class JsonByteArray implements ByteArray {

  private final byte[] data;

  public JsonByteArray(byte[] data) {
    this.data = data.clone();
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
  public int indexOf(double target) {
    byte b = (byte) target;
    for (int i = 0; i < data.length; i++) {
      if (data[i] == b) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int lastIndexOf(double target) {
    byte b = (byte) target;
    for (int i = data.length - 1; i >= 0; i--) {
      if (data[i] == b) {
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
    return new JsonByteArray(Arrays.copyOfRange(data, startIndexInclusive, endIndexExclusive));
  }

  @Override
  public List<Integer> asList() {
    List<Integer> list = new ArrayList<>(data.length);
    for (byte b : data) {
      list.add((int) b);
    }
    return list;
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
    return obj instanceof JsonByteArray other && Arrays.equals(this.data, other.data);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }

  @Override
  public String toString() {
    return Arrays.toString(data);
  }

  /** Serializes {@link JsonByteArray} as a Base64-encoded string in JSON. */
  public static final class Serializer extends JsonSerializer<JsonByteArray> {
    @Override
    public void serialize(JsonByteArray value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      gen.writeBinary(value.data);
    }
  }

  /** Deserializes a Base64-encoded JSON string into {@link JsonByteArray}. */
  public static final class Deserializer extends JsonDeserializer<JsonByteArray> {
    @Override
    public JsonByteArray deserialize(JsonParser p, DeserializationContext context)
        throws IOException {
      return new JsonByteArray(p.getBinaryValue());
    }
  }
}
