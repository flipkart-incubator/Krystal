package com.flipkart.krystal.data;

import com.flipkart.krystal.concurrent.Continuation;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A wrapper class for a graph execution request and its response.
 *
 * @param facetValues
 * @param response
 */
public record ExecutionItem(
    FacetValuesBuilder facetValues, Continuation<@Nullable Object> response) {}
