package com.flipkart.krystal.vajram.batching;

import com.google.common.collect.ImmutableList;

/**
 * A list of {@link BatchEnabledFacetValues} which have been batched together by an {@link
 * InputBatcher} on the condition that all facets in {@link #batchItems()} have {@link
 * BatchEnabledFacetValues#_batchKey()} that are equal to each other. The number of elements in the
 * {@link #batchItems()} list depends on the batch size and other such configurations of the {@link
 * InputBatcher}
 */
public record BatchedFacets(ImmutableList<BatchEnabledFacetValues> batchItems) {}
