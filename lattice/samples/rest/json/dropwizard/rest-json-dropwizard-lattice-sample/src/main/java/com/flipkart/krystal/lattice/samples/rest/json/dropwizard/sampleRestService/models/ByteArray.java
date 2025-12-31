package com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.models;

import java.nio.charset.Charset;
import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.Nullable;

public record ByteArray(byte[] b) {

  public String toString(Charset charset) {
    return new String(b, charset);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
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
