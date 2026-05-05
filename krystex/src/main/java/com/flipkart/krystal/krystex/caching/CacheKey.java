package com.flipkart.krystal.krystex.caching;

import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.ImmutableFacetValuesContainer;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * We use facets instead of request as a cache key so that some non-input facets which have been
 * specifically added (for example, injections) to act as cache keys are also taken into account.
 * This, however, is a rare use case and in most cases, all non-input facets are null at the time of
 * cache key computation and using the facets object is equivalent to using the inner request
 * object.
 *
 * @param facets
 */
@Slf4j
record CacheKey(ImmutableFacetValuesContainer facets) {

  public static @Nullable CacheKey newCacheKey(FacetValues facetValues) {
    ImmutableFacetValuesContainer immut;
    try {
      immut = facetValues._build();
    } catch (Exception e) {
      log.error(
          "Unable to generate cache key by 'building' facet values to create an Immutable instance as an exception was encountered while building.",
          e);
      return null;
    }
    return new CacheKey(immut);
  }
}
