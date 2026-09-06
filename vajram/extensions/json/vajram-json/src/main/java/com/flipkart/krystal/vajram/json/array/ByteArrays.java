package com.flipkart.krystal.vajram.json.array;

import com.flipkart.krystal.model.array.ByteArray;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;

public class ByteArrays {
  /** Serializes {@link JsonByteArray} as a Base64-encoded string in JSON. */
  public static final class ByteArraySerializer extends ValueSerializer<ByteArray> {
    @Override
    public void serialize(ByteArray value, JsonGenerator gen, SerializationContext serializers) {
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
  public static final class ByteArrayDeserializer extends ValueDeserializer<JsonByteArray> {
    @Override
    public JsonByteArray deserialize(JsonParser p, DeserializationContext context) {
      // Avoid copying of bytes by using of() instead of copyOf() since the binary value byte array
      // would most probably not be modified after this
      return JsonByteArray.backedBy(p.getBinaryValue());
    }
  }
}
