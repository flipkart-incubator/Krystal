package com.flipkart.krystal.data;

/**
 * Represents a response from a vajram when it is invoked as a dependency by another vajram
 *
 * @param <T> The type of the dependency response
 * @param <R> The request type of the dependency Vajram
 */
public sealed interface DepResponse<R extends Request<T>, T> extends FacetValue<T>
    permits FanoutDepResponses, One2OneDepResponse {}
