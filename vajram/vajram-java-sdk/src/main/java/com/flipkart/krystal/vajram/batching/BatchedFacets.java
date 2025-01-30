package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.FacetContainer;
import com.google.common.collect.ImmutableList;

public record BatchedFacets<Batched extends BatchedFacetsElement, Common extends FacetContainer>(
    ImmutableList<Batched> batchItems, Common common) {}
