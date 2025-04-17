package com.flipkart.krystal.vajram.facets.specs;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.DepResponse;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FacetValuesBuilder;
import com.flipkart.krystal.data.One2OneDepResponse;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponse;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.tags.ElementTags;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.Getter;

/**
 * Represents a dependency vajram which can be invoked exactly once (no fanout) by the current
 * vajram.
 *
 * @param <T> The return type of the dependency vajram
 * @param <CV> The current vajram which has the dependency
 * @param <DV> The dependency vajram
 */
@Getter
public abstract sealed class One2OneDepSpec<T, CV extends Request, DV extends Request<T>>
    extends DependencySpec<T, CV, DV> permits MandatoryOne2OneDepSpec, OptionalOne2OneDepSpec {

  private final Function<FacetValues, One2OneDepResponse<DV, T>> getFromFacets;
  private final BiConsumer<FacetValues, RequestResponse<DV, T>> setToFacets;

  public One2OneDepSpec(
      int id,
      String name,
      VajramID ofVajramID,
      DataType<T> type,
      Class<CV> ofVajram,
      Class<DV> onVajram,
      VajramID onVajramId,
      String documentation,
      boolean isBatched,
      Callable<ElementTags> tagsParser,
      Function<FacetValues, One2OneDepResponse<DV, T>> getFromFacets,
      BiConsumer<FacetValues, RequestResponse<DV, T>> setToFacets) {
    super(
        id,
        name,
        ofVajramID,
        type,
        ofVajram,
        onVajram,
        onVajramId,
        documentation,
        isBatched,
        tagsParser);
    this.getFromFacets = getFromFacets;
    this.setToFacets = setToFacets;
  }

  @Override
  public One2OneDepResponse getFacetValue(FacetValues facetValues) {
    return getFromFacets.apply(facetValues);
  }

  @Override
  @SuppressWarnings("unchecked")
  public final void setFacetValue(FacetValuesBuilder facets, DepResponse value) {
    if (value instanceof RequestResponse requestResponse) {
      setFacetValue(facets, (RequestResponse<DV, T>) requestResponse);
    } else {
      throw new RuntimeException(
          "One2One Dependency expects facet value of type RequestResponse. Found "
              + value.getClass());
    }
  }

  @SuppressWarnings("MethodOverloadsMethodOfSuperclass")
  public final void setFacetValue(FacetValuesBuilder facets, RequestResponse<DV, T> value) {
    setToFacets.accept(facets, value);
  }

  @Override
  public One2OneDepResponse<DV, T> getPlatformDefaultValue() throws UnsupportedOperationException {
    return One2OneDepResponse.noRequest();
  }

  @Override
  public boolean canFanout() {
    return false;
  }
}
