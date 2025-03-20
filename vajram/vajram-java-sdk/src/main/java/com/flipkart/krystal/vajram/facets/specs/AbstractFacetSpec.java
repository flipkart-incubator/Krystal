package com.flipkart.krystal.vajram.facets.specs;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.AbstractFacet;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;

/**
 * Represents a facet of the current vajram. This may represent an input of this vajram or a
 * depenedency of this vajram (See: {@link DependencySpec})
 *
 * @param <T> The data type of the facet.
 * @param <CV> The current vajram which has the facet
 */
@Getter
public abstract sealed class AbstractFacetSpec<T, CV extends Request> extends AbstractFacet
    implements FacetSpec<T, CV> permits DefaultFacetSpec, DependencySpec {

  private final DataType<T> type;
  private final boolean isBatched;
  private final Class<CV> ofVajram;
  private final ElementTags tags;

  public AbstractFacetSpec(
      int id,
      String name,
      VajramID ofVajramID,
      DataType<T> type,
      ImmutableSet<FacetType> facetTypes,
      Class<CV> ofVajram,
      String documentation,
      boolean isBatched,
      ElementTags tags) {
    super(id, name, ofVajramID, facetTypes, documentation);
    this.type = type;
    this.ofVajram = ofVajram;
    this.tags = tags;
    this.isBatched = isBatched;
  }
}
