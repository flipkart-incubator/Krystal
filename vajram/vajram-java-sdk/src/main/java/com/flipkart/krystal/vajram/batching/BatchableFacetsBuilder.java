package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.FacetsBuilder;

public interface BatchableFacetsBuilder extends BatchableFacets, FacetsBuilder {

  FacetsBuilder _batchable();

  FacetsBuilder _common();
}
