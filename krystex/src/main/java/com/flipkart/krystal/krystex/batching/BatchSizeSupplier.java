package com.flipkart.krystal.krystex.batching;

import com.flipkart.krystal.core.VajramID;

@FunctionalInterface
public interface BatchSizeSupplier {
  int getBatchSize(VajramID vajramId);
}
