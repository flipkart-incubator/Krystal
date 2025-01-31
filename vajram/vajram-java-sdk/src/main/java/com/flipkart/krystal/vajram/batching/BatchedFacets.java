package com.flipkart.krystal.vajram.batching;

import com.google.common.collect.ImmutableList;

/**
 * A list of {@link BatchEnabledFacets} which have been batched together by an {@link InputBatcher}
 * on the condition that all facets in {@link #batchItems()} have {@link
 * BatchEnabledFacets#_common()} that are equal to each other. The number of elements in the {@link
 * #batchItems()} list depends on the batch size and other such configurations of the {@link
 * InputBatcher}
 *
 * @param batchItems
 */
public record BatchedFacets(ImmutableList<BatchEnabledFacets> batchItems) {}
