package com.flipkart.krystal.vajram.json.serialized;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.io.CharSource;
import java.io.IOException;
import java.io.InputStream;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class StringJson extends AbstractJsonRepresentation {

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
  public InputStream newInputStream() {
    try {
      return CharSource.wrap(string).asByteSource(UTF_8).openStream();
    } catch (IOException e) {
      throw new ArrayStoreException("String CharSource cannot throw exception when opening Stream");
    }
  }

  @Override
  public String asString() {
    return string;
  }

  protected byte[] asBytes() {
    if (bytes == null) {
      bytes = string.getBytes(UTF_8);
    }
    return bytes;
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
