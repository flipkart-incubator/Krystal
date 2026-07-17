package com.flipkart.krystal.vajram.json.serialized;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

abstract sealed class AbstractJsonRepresentation implements JsonRepresentation
    permits ByteArrayJson, BytesJson, NodeJson, StringJson {

  /* ----Derived fields ----- */
  protected String string;

  @Override
  public InputStream newInputStream() {
    return new ByteArrayInputStream(asBytes());
  }

  @Override
  public String asString() {
    if (string == null) {
      string = new String(asBytes(), UTF_8);
    }
    return string;
  }

  protected abstract byte[] asBytes();
}
