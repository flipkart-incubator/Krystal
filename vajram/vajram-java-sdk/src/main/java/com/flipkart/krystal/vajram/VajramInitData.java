package com.flipkart.krystal.vajram;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.ImmutableRequest;
import java.util.Map;
import java.util.function.Supplier;

public record VajramInitData(
    Map<VajramID, Supplier<? extends ImmutableRequest.Builder<?>>> vajramRequestBuilderSuppliers) {

  public VajramInitData() {
    this(Map.of());
  }
}
