package com.flipkart.krystal.vajram.json.serialized;

import com.fasterxml.jackson.databind.ObjectReader;
import com.flipkart.krystal.model.array.ByteArray;
import com.flipkart.krystal.vajram.json.array.JsonByteArray;
import java.io.IOException;
import java.io.InputStream;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class ByteArrayJson extends AbstractJsonRepresentation {

  private final ByteArray data;

  /* ---------- DERIVED FIELDS ----------- */
  private byte @MonotonicNonNull [] bytes;

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
  public InputStream newInputStream() {
    return data.newInputStream();
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

  @Override
  protected byte[] asBytes() {
    if (bytes == null) {
      bytes = data.toArray();
    }
    return bytes;
  }
}
