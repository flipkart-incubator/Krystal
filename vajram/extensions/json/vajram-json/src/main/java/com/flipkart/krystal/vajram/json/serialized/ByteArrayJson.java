package com.flipkart.krystal.vajram.json.serialized;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.ObjectReader;
import com.flipkart.krystal.model.array.ByteArray;
import com.flipkart.krystal.vajram.json.array.JsonByteArray;
import java.io.IOException;
import java.io.OutputStream;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class ByteArrayJson implements SerializedJson {

  private final ByteArray data;

  /* ---------- DERIVED FIELDS ----------- */
  private byte @MonotonicNonNull [] bytes;
  private @MonotonicNonNull String string;

  public ByteArrayJson(ByteArray data) {
    this.data = data;
  }

  @Override
  public <T> T deserialize(ObjectReader reader) throws IOException {
    if (data instanceof JsonByteArray jsonByteArray) {
      return jsonByteArray.readFromJson(reader);
    } else {
      return reader.readValue(data.newInputStream());
    }
  }

  @Override
  public byte[] asBytes() {
    if (bytes == null) {
      bytes = data.toArray();
    }
    return bytes.clone();
  }

  @Override
  public String asString() {
    if (string == null) {
      string = new String(asBytes(), UTF_8);
    }
    return string;
  }

  @Override
  public void writeTo(OutputStream outputStream) throws IOException {
    outputStream.write(asBytes());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    return obj instanceof ByteArrayJson byteArrayJson && this.data.equals(byteArrayJson.data);
  }

  @Override
  public int hashCode() {
    return data.hashCode();
  }
}
