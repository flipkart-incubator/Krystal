package com.flipkart.krystal.facets;

/**
 * Marker interface for Vajram or Trait inputs defined outside the vajram class. Classes extending
 * this interface must have the {@link InputsForVajram} annotation to participate in code
 * generation.
 *
 * @param <T> The response type of the vajram or trait
 */
public interface VajramInputs<T> {}
