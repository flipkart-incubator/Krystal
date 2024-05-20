package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.batching.FacetsConverter;
import com.flipkart.krystal.vajram.facets.FacetValuesAdaptor;
import java.util.Optional;

public sealed interface BatchableVajram<T> extends Vajram<T> permits IOVajram {
  default Optional<
          ? extends FacetsConverter<? extends FacetValuesAdaptor, ? extends FacetValuesAdaptor>>
      getBatchFacetsConvertor() {
    // Case where BatchableVajram implementation doesn't configure batching
    return Optional.empty();
  }
}
