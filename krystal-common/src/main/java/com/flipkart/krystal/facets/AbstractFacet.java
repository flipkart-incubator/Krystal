package com.flipkart.krystal.facets;

import com.google.common.collect.ImmutableSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

@Getter
@AllArgsConstructor
public abstract class AbstractFacet implements Facet {
  private final int id;
  private final String name;
  private final ImmutableSet<FacetType> facetTypes;
  private final String documentation;

  @Override
  public final boolean equals(@Nullable Object obj) {
    return obj instanceof Facet f && f.id() == id;
  }

  @Override
  public final int hashCode() {
    return id;
  }

  @Override
  public String toString() {
    return "Facet(" + id() + ", " + name() + ", " + facetTypes() + ')';
  }
}
