package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.FacetContainerBuilder;
import com.flipkart.krystal.data.FacetsBuilder;

public interface BatchableFacetsBuilder extends BatchableFacets, FacetsBuilder {

  FacetContainerBuilder _batchable();

  FacetContainerBuilder _common();
}
