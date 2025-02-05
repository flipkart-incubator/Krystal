package com.flipkart.krystal.vajram.facets.specs;

import com.flipkart.krystal.data.FacetValues;
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
public final class MandatoryFanoutDepSpec<T, CV extends Request, DV extends Request<T>>
    extends FanoutDepSpec<T, CV, DV> implements MandatoryFacetSpec<T, CV> {

  public MandatoryFanoutDepSpec(
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
    super(
        id,
        name,
        type,
        ofVajram,
        onVajram,
        onVajramId,
        documentation,
        isBatched,
        tags,
        getFromFacets,
        setToFacets);
  }
}
