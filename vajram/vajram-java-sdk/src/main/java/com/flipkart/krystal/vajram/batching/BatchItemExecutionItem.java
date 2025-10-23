package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.data.ImmutableFacetValuesContainer;

public record BatchItemExecutionItem(
    ImmutableFacetValuesContainer batchItem, ExecutionItem executionItem) {}
