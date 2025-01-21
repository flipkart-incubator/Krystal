package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.FacetContainer;
import com.google.common.collect.ImmutableList;

public record BatchedFacets<Batched extends FacetContainer, Common extends FacetContainer>(
    ImmutableList<Batched> batch, Common common) {}
