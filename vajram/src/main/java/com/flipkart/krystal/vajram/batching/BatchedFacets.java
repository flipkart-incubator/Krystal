package com.flipkart.krystal.vajram.batching;

import com.google.common.collect.ImmutableList;

public record BatchedFacets<Batched, Common>(ImmutableList<Batched> batch, Common commonFacets) {}
