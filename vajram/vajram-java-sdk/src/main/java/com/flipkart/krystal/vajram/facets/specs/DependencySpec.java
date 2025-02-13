package com.flipkart.krystal.vajram.facets.specs;

import static com.flipkart.krystal.facets.FacetType.DEPENDENCY;

import com.flipkart.krystal.data.DepResponse;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FacetValuesBuilder;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.vajram.VajramID;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;

/**
 * Represents a dependency facet of the current vajram (with request CV) which depends on the vajram
 * (with request type DV).
 *
 * @param <T> The type of this facet - this is the return type of the dependency vajram
 * @param <CV> The request type of the current vajram which has the dependency
 * @param <DV> The request type of the dependency vajram
 */
@Getter
public abstract sealed class DependencySpec<T, CV extends Request, DV extends Request<T>>
    extends AbstractFacetSpec<T, CV> implements Dependency permits FanoutDepSpec, One2OneDepSpec {

  private final Class<DV> onVajram;
  private final VajramID onVajramId;

  public DependencySpec(
      int id,
      String name,
      DataType<T> dataType,
      Class<CV> ofVajram,
      Class<DV> onVajram,
      VajramID onVajramId,
      String documentation,
      boolean isBatched,
      ElementTags tags) {
    super(
        id, name, dataType, ImmutableSet.of(DEPENDENCY), ofVajram, documentation, isBatched, tags);
    this.onVajram = onVajram;
    this.onVajramId = onVajramId;
  }

  @Override
  @SuppressWarnings("unchecked")
  public final void setFacetValue(FacetValuesBuilder facets, FacetValue value) {
    if (value instanceof DepResponse depResponse) {
      setFacetValue(facets, (DepResponse<DV, T>) depResponse);
    } else {
      throw new RuntimeException(
          "Dependency expects facet value of type DepResponse. Found " + value.getClass());
    }
  }

  protected abstract void setFacetValue(FacetValuesBuilder facets, DepResponse<DV, T> depResponse);

  @Override
  public abstract DepResponse getFacetValue(FacetValues facetValues);
}
