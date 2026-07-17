package com.flipkart.krystal.vajram.json.serialized;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.flipkart.krystal.model.array.ByteArray;
import java.io.IOException;
import java.io.InputStream;

public sealed interface JsonRepresentation permits AbstractJsonRepresentation {
  <T> T deserialize(ObjectReader reader) throws IOException;

  InputStream newInputStream();

  String asString();

  default boolean isReusable() {
    return true;
  }

  static JsonRepresentation of(Object payload) throws IOException {
    JsonRepresentation jsonRepresentation;
    if (payload instanceof InputStream inputStream) {
      jsonRepresentation = new BytesJson(inputStream);
    } else if (payload instanceof byte[] bytes) {
      jsonRepresentation = new BytesJson(bytes);
    } else if (payload instanceof String string) {
      jsonRepresentation = new StringJson(string);
    } else if (payload instanceof ByteArray byteArray) {
      jsonRepresentation = new ByteArrayJson(byteArray);
    } else if (payload instanceof JsonNode jsonNode) {
      jsonRepresentation = new NodeJson(jsonNode);
    } else {
      throw new IllegalArgumentException("Unsupported payload type: " + payload.getClass());
    }
    return jsonRepresentation;
  }
}
