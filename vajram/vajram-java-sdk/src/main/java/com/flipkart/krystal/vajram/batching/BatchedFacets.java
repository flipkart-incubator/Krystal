package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.Facets;
import com.google.common.collect.ImmutableList;

public record BatchedFacets<Batched extends Facets, Common extends Facets>(
    ImmutableList<Batched> batch, Common common) {}
