package com.flipkart.krystal.vajram.facets;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import com.flipkart.krystal.schema.FacetSpec;
import lombok.EqualsAndHashCode;

/**
 * Represents a facet of the current vajram. This may also represent an input of this vajram or a
 * depenedency of this vajram (See: {@link VajramDependencySpec})
 *
 * @param <T> The data type of the facet.
 */
@EqualsAndHashCode(cacheStrategy = LAZY)
public sealed class VajramFacetSpec<T> implements FacetSpec<T> permits VajramDependencySpec {

  private final String name;

  public VajramFacetSpec(String name) {
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }
}
