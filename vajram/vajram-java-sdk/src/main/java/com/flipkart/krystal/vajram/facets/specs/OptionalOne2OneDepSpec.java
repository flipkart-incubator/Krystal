package com.flipkart.krystal.vajram.facets.specs;

import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.One2OneDepResponse;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponse;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.vajram.VajramID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public non-sealed class OptionalOne2OneDepSpec<T, CV extends Request, DV extends Request<T>>
    extends One2OneDepSpec<T, CV, DV> implements OptionalFacetSpec<T, CV> {

  public OptionalOne2OneDepSpec(
      int id,
      String name,
      DataType<T> type,
      Class<CV> ofVajram,
      Class<DV> onVajram,
      VajramID onVajramId,
      String documentation,
      boolean isBatched,
      ElementTags tags,
      Function<FacetValues, One2OneDepResponse<DV, T>> getFromFacets,
      BiConsumer<FacetValues, RequestResponse<DV, T>> setToFacets) {
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
