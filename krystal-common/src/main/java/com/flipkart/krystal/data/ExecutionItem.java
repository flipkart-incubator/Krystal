package com.flipkart.krystal.data;

import java.util.concurrent.CompletableFuture;

/**
 * A wrapper class for a graph execution request and its response.
 *
 * @param facetValues
 * @param response
 * @param <T> The response type
 */
public record ExecutionItem<T>(FacetValues facetValues, CompletableFuture<T> response) {}
