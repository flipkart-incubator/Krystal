package com.flipkart.krystal.vajram.json.serialized;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.ObjectReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public final class BytesJson extends AbstractJsonRepresentation {
  private final byte[] data;
  private final int start;
  private final int length;

  public BytesJson(InputStream inputStream) throws IOException {
    this(inputStream.readAllBytes());
  }

  public BytesJson(byte[] data) {
    this(data, 0, data.length);
  }

  public BytesJson(byte[] data, int start, int length) {
    this.data = data;
    this.start = start;
    this.length = length;
  }

  @Override
  public <T> T deserialize(ObjectReader reader) throws IOException {
    return reader.readValue(data, start, length);
  }

  @Override
  public InputStream newInputStream() {
    return new ByteArrayInputStream(data, start, length);
  }

  @Override
  public String asString() {
    if (string == null) {
      string = new String(data, start, length, UTF_8);
    }
    return string;
  }

  @Override
  protected byte[] asBytes() {
    byte[] bytes;
    if (start == 0 && length == data.length) {
      bytes = data;
    } else {
      bytes = Arrays.copyOfRange(data, start, start + length);
    }
    return bytes;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    return obj instanceof BytesJson bytesJson
        && ((this.data == bytesJson.data
                && this.start == bytesJson.start
                && this.length == bytesJson.length)
            || Arrays.equals(
                this.data,
                this.start,
                this.start + this.length,
                bytesJson.data,
                bytesJson.start,
                bytesJson.start + bytesJson.length));
  }

  @Override
  public int hashCode() {
    int result = 1;
    for (int i = start; i < start + length; i++) {
      result = 31 * result + data[i];
    }
    return result;
  }

  @Override
  public String toString() {
    return "BytesJson[data=" + data + ", start= " + start + ", length= " + length + "]";
  }
}
