package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.batching.BatchableSupplier;

public non-sealed interface BatchableVajram<T> extends Vajram<T> {
  BatchableSupplier<?, ?> getBatchFacetsConvertor();
}
