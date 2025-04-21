package com.flipkart.krystal.data;

/**
 * @param <T> The type of the dependency response
 * @param <R> The request type of the dependency Vajram
 */
public sealed interface DepResponse<R extends Request<T>, T> extends FacetValue<T>
    permits FanoutDepResponses, One2OneDepResponse {}
