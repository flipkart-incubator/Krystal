package com.flipkart.krystal.data;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a response from a vajram when it is invoked as a dependency by another vajram
 *
 * @param <T> The type of the dependency response
 * @param <R> The request type of the dependency Vajram
 */
public sealed interface DepResponse<R extends Request<@Nullable T>, T> extends FacetValue<T>
    permits FanoutDepResponses, One2OneDepResponse {}
