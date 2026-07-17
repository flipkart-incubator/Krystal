package com.flipkart.krystal.vajram.json.serialized;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.io.OutputStream;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class StringJson implements SerializedJson {

  private final String string;

  /* ---------- DERIVED FIELDS ----------- */
  private byte @MonotonicNonNull [] bytes;

  public StringJson(String string) {
    this.string = string;
  }

  @Override
  public <T> T deserialize(ObjectReader reader) throws IOException {
    if (bytes != null) {
      return reader.readValue(bytes);
    }
    return reader.readValue(string);
  }

  @Override
  public byte[] asBytes() {
    if (bytes == null) {
      bytes = string.getBytes(UTF_8);
    }
    return bytes.clone();
  }

  @Override
  public String asString() {
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
    return obj instanceof StringJson stringJson && string.equals(stringJson.string);
  }

  @Override
  public int hashCode() {
    return string.hashCode();
  }
}
