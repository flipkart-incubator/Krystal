package com.flipkart.krystal.vajram.batching;

import com.google.common.collect.ImmutableList;

public record BatchedFacets<BatchableInputs, CommonFacets>(
    ImmutableList<BatchableInputs> batchedInputs, CommonFacets commonFacets) {}
