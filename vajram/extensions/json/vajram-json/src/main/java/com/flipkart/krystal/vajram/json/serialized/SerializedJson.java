package com.flipkart.krystal.vajram.json.serialized;

import com.fasterxml.jackson.databind.ObjectReader;
import com.flipkart.krystal.model.array.ByteArray;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public sealed interface SerializedJson permits BytesJson, StringJson, ByteArrayJson {
  <T> T deserialize(ObjectReader reader) throws IOException;

  byte[] asBytes();

  String asString();

  void writeTo(OutputStream outputStream) throws IOException;

  default boolean isReusable() {
    return true;
  }

  static SerializedJson of(Object payload) throws IOException {
    SerializedJson serializedJson;
    if (payload instanceof InputStream inputStream) {
      serializedJson = new BytesJson(inputStream);
    } else if (payload instanceof byte[] bytes) {
      serializedJson = new BytesJson(bytes);
    } else if (payload instanceof String string) {
      serializedJson = new StringJson(string);
    } else if (payload instanceof ByteArray byteArray) {
      serializedJson = new ByteArrayJson(byteArray);
    } else {
      throw new IllegalArgumentException("Unsupported payload type: " + payload.getClass());
    }
    return serializedJson;
  }
}
