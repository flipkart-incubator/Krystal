package com.flipkart.krystal.vajram;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.vajram.batching.BatchableSupplier;
import java.util.Optional;

public sealed interface BatchableVajram<T> extends Vajram<T> permits IOVajram {
  default Optional<? extends BatchableSupplier<? extends Facets, ? extends Facets>>
      getBatchFacetsConvertor() {
    // Case where BatchableVajram implementation doesn't configure batching
    return Optional.empty();
  }
}
