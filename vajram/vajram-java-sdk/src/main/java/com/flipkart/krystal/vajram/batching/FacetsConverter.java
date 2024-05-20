package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.vajram.facets.FacetValuesAdaptor;

public interface FacetsConverter<
    BatchedFacets extends FacetValuesAdaptor, CommonFacets extends FacetValuesAdaptor> {

  /** Extracts the batched facets and returns an object containing only these facets */
  BatchedFacets getBatched(Facets facets);

  /** Extracts the common facets and returns an object containing only these facets */
  CommonFacets getCommon(Facets facets);
}
