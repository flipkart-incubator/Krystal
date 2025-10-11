package com.flipkart.krystal.data;

import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A wrapper class for a graph execution request and its response.
 *
 * @param facetValues
 * @param response
 */
public record ExecutionItem(
    FacetValuesBuilder facetValues, CompletableFuture<@Nullable Object> response) {}
