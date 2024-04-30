package com.flipkart.krystal.vajram;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.vajram.batching.BatchableSupplier;
import java.util.Optional;

public non-sealed interface BatchableVajram<T> extends Vajram<T> {
  BatchableSupplier<? extends Facets, ? extends Facets> getBatchFacetsConvertor();
}
