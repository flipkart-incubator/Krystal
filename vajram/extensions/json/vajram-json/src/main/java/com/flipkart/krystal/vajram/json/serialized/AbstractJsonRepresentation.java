package com.flipkart.krystal.vajram.json.serialized;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

abstract sealed class AbstractJsonRepresentation implements JsonRepresentation
    permits ByteArrayJson, BytesJson, NodeJson, StringJson {

  /* ----Derived fields ----- */
  protected @MonotonicNonNull String string;

  @Override
  public InputStream _serialize() {
    return new ByteArrayInputStream(asBytes());
  }

  @Override
  public String _asString() {
    if (string == null) {
      string = new String(asBytes(), UTF_8);
    }
    return string;
  }

  protected abstract byte[] asBytes();
}
