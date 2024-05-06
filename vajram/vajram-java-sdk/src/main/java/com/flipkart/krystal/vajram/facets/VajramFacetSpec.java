package com.flipkart.krystal.vajram.facets;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.schema.FacetSpec;
import lombok.EqualsAndHashCode;

/**
 * Represents a facet of the current vajram. This may represent an input of this vajram or a
 * depenedency of this vajram (See: {@link VajramDependencySpec})
 *
 * @param <T> The data type of the facet.
 */
@EqualsAndHashCode(cacheStrategy = LAZY)
public sealed class VajramFacetSpec<T, CV extends Request<?>> implements FacetSpec<T>
    permits VajramDependencySpec {

  private final int id;
  private final String name;

  public VajramFacetSpec(int id, String name, Class<CV> ofVajram) {
    this.id = id;
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  public int id() {
    return id;
  }
}
