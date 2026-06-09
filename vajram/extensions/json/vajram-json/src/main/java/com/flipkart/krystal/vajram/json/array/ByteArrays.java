package com.flipkart.krystal.vajram.json.array;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.flipkart.krystal.model.array.ByteArray;
import java.io.IOException;

public class ByteArrays {
  /** Serializes {@link JsonByteArray} as a Base64-encoded string in JSON. */
  public static final class ByteArraySerializer extends JsonSerializer<ByteArray> {
    @Override
    public void serialize(ByteArray value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      if (value instanceof JsonByteArray jsonByteArray) {
        // Performant since there is no data copy involved
        jsonByteArray.writeBase64ToJson(gen);
        return;
      } else {
        // Less performant because bytes from input stream are copied into a new array before
        // deserialization
        gen.writeBinary(value.newInputStream(), value.length());
      }
    }
  }

  /** Deserializes a Base64-encoded JSON string into {@link JsonByteArray}. */
  public static final class ByteArrayDeserializer extends JsonDeserializer<JsonByteArray> {
    @Override
    public JsonByteArray deserialize(JsonParser p, DeserializationContext context)
        throws IOException {
      // Avoid copying of bytes by using of() instead of copyOf() since the binary value byte array
      // would not be modified after this
      return JsonByteArray.of(p.getBinaryValue());
    }
  }
}
