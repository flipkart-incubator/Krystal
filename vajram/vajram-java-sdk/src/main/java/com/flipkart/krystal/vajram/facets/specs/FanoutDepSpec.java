package com.flipkart.krystal.vajram.facets.specs;

import com.flipkart.krystal.data.DepResponse;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FacetValuesBuilder;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.vajram.VajramID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.Getter;

/**
 * Represents a dependency vajram which can be invoked a variable number of times (fanout) by the
 * current vajram.
 *
 * @param <T> The return type of the dependency vajram
 * @param <CV> The current vajram which has the dependency
 * @param <DV> The dependency vajram
 */
@Getter
public abstract sealed class FanoutDepSpec<T, CV extends Request, DV extends Request<T>>
    extends DependencySpec<T, CV, DV> permits MandatoryFanoutDepSpec, OptionalFanoutDepSpec {

  private final Function<FacetValues, FanoutDepResponses<DV, T>> getFromFacets;
  private final BiConsumer<FacetValues, FanoutDepResponses<DV, T>> setToFacets;

  public FanoutDepSpec(
      int id,
      String name,
      DataType<T> type,
      Class<CV> ofVajram,
      Class<DV> onVajram,
      VajramID onVajramId,
      String documentation,
      boolean isBatched,
      ElementTags tags,
      Function<FacetValues, FanoutDepResponses<DV, T>> getFromFacets,
      BiConsumer<FacetValues, FanoutDepResponses<DV, T>> setToFacets) {
    super(id, name, type, ofVajram, onVajram, onVajramId, documentation, isBatched, tags);
    this.getFromFacets = getFromFacets;
    this.setToFacets = setToFacets;
  }

  @Override
  public FanoutDepResponses<DV, T> getFacetValue(FacetValues facetValues) {
    return getFromFacets.apply(facetValues);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setFacetValue(FacetValuesBuilder facets, DepResponse<DV, T> value) {
    if (value instanceof FanoutDepResponses fanoutDepResponses) {
      setFacetValue(facets, (FanoutDepResponses<DV, T>) fanoutDepResponses);
    } else {
      throw new RuntimeException(
          "Expecting facet value type 'DepResponse' for dependency facet, but found: "
              + value.getClass());
    }
  }

  @SuppressWarnings({"MethodOverloadsMethodOfSuperclass", "unchecked"})
  public void setFacetValue(FacetValuesBuilder facets, FanoutDepResponses<DV, T> value) {
    setToFacets.accept(facets, (FanoutDepResponses<DV, T>) value);
  }

  @Override
  public final FanoutDepResponses<DV, T> getPlatformDefaultValue()
      throws UnsupportedOperationException {
    return FanoutDepResponses.empty();
  }

  @Override
  public boolean canFanout() {
    return true;
  }
}
