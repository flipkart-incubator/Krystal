package com.flipkart.krystal.vajram.json.serialized;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.io.CharSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class StringJson extends AbstractJsonRepresentation {

  private static final int VERY_LARGE_STRING_MIN_SIZE = 10_000_000; // 10 million characters long

  private final String string;
  private final boolean isSmallString;

  /* ---------- DERIVED DATA FIELDS ----------- */
  private byte @MonotonicNonNull [] bytes;

  public StringJson(String string) {
    this.string = string;
    isSmallString = string.length() <= VERY_LARGE_STRING_MIN_SIZE;
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
    if (isSmallString || bytes != null) {
      // For small strings, it's more performant to just convert the whole string to a byte array
      // and cache it
      return new ByteArrayInputStream(asBytes());
    } else {
      // openStream method allocates a byte buffer of size 8192 each time it is called and the
      // byte source decodes the string each time which takes some CPU. So this
      // code-path is preferable only if the strings are very large and allocating a new buffer each
      // time newInputStream() is called is more performant than copying the complete string into a
      // new byte[] once
      try {
        return CharSource.wrap(string).asByteSource(UTF_8).openStream();
      } catch (IOException e) {
        throw new AssertionError("String CharSource cannot throw exception when opening Stream");
      }
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
