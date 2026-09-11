package com.flipkart.krystal.vajram.json.array;

import com.flipkart.krystal.model.array.FloatArray;
import com.flipkart.krystal.model.array.SimpleFloatArray;
import org.checkerframework.checker.nullness.qual.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;

public class FloatArrays {
  /** Serializes {@link JsonByteArray} as a Base64-encoded string in JSON. */
  public static final class FloatArraySerializer extends ValueSerializer<FloatArray> {
    @Override
    public void serialize(FloatArray value, JsonGenerator gen, SerializationContext serializers) {
      gen.writeStartArray();
      value.forEach(gen::writeNumber);
      gen.writeEndArray();
    }
  }

  /** Deserializes a Base64-encoded JSON string into {@link SimpleFloatArray}. */
  public static final class FloatArrayDeserializer
      extends ValueDeserializer<@Nullable SimpleFloatArray> {
    @Override
    public @Nullable SimpleFloatArray deserialize(JsonParser p, DeserializationContext context) {
      float[] data = p.readValueAs(float[].class);
      if (data == null) {
        return null;
      }
      // Avoid copying of floats by using backedBy() instead of copyOf() since the float
      // array cannot be modified after this
      return SimpleFloatArray.backedBy(data);
    }
  }
}
