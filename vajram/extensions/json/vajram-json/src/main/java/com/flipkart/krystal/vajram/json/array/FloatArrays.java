package com.flipkart.krystal.vajram.json.array;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.flipkart.krystal.model.array.FloatArray;
import com.flipkart.krystal.model.array.SimpleFloatArray;
import java.io.IOException;
import java.io.UncheckedIOException;

public class FloatArrays {
  /** Serializes {@link JsonByteArray} as a Base64-encoded string in JSON. */
  public static final class FloatArraySerializer extends JsonSerializer<FloatArray> {
    @Override
    public void serialize(FloatArray value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      gen.writeStartArray();
      try {
        value.forEach(
            fVal -> {
              try {
                gen.writeNumber(fVal);
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });
      } catch (UncheckedIOException e) {
        throw e.getCause();
      }
      gen.writeEndArray();
    }
  }

  /** Deserializes a Base64-encoded JSON string into {@link SimpleFloatArray}. */
  public static final class FloatArrayDeserializer extends JsonDeserializer<SimpleFloatArray> {
    @Override
    public SimpleFloatArray deserialize(JsonParser p, DeserializationContext context)
        throws IOException {
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
