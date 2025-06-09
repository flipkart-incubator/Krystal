package com.flipkart.krystal.lattice.samples.rest.json.sampleRestService;

import java.nio.charset.Charset;
import java.util.Arrays;

public record ByteArray(byte[] b) {

  public String toString(Charset charset) {
    return new String(b, charset);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ByteArray byteArray && Arrays.equals(this.b, byteArray.b);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(b);
  }

  @Override
  public String toString() {
    return Arrays.toString(b);
  }
}
