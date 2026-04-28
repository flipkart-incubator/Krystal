package com.flipkart.krystal.vajram.json.array;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.flipkart.krystal.model.array.SimpleByteArray;
import java.io.IOException;

public class ByteArrays {
  /** Serializes {@link SimpleByteArray} as a Base64-encoded string in JSON. */
  public static final class ByteArraySerializer extends JsonSerializer<SimpleByteArray> {
    @Override
    public void serialize(SimpleByteArray value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      gen.writeBinary(value.newInputStream(), value.length());
    }
  }

  /** Deserializes a Base64-encoded JSON string into {@link SimpleByteArray}. */
  public static final class ByteArrayDeserializer extends JsonDeserializer<SimpleByteArray> {
    @Override
    public SimpleByteArray deserialize(JsonParser p, DeserializationContext context)
        throws IOException {
      return SimpleByteArray.copyOf(p.getBinaryValue());
    }
  }
}
