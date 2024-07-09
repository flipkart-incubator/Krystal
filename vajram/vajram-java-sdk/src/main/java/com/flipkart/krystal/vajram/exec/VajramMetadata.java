package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.InputDef;
import com.flipkart.krystal.vajram.facets.InputSource;

public record VajramMetadata(boolean isInputInjectionNeeded) {

  public VajramMetadata(Vajram<?> vajram) {
    this(
        /* isInputInjectionNeeded= */ vajram.getFacetDefinitions().stream()
            .filter(facetDefinition -> facetDefinition instanceof InputDef<?>)
            .map(facetDefinition -> ((InputDef<?>) facetDefinition))
            .anyMatch(
                input -> input.sources() != null && input.sources().contains(InputSource.SESSION)));
  }
}
