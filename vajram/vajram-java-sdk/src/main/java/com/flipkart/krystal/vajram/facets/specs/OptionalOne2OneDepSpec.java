package com.flipkart.krystal.vajram.facets.specs;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.One2OneDepResponse;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.tags.ElementTags;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Function;

public non-sealed class OptionalOne2OneDepSpec<T, CV extends Request, DV extends Request<T>>
    extends One2OneDepSpec<T, CV, DV> implements OptionalSingleValueFacetSpec<T, CV> {

  public OptionalOne2OneDepSpec(
      int id,
      String name,
      VajramID vajramID,
      DataType<T> type,
      Class<CV> ofVajram,
      Class<?> onVajram,
      VajramID onVajramId,
      String documentation,
      boolean isBatched,
      Callable<ElementTags> tagsParser,
      Function<FacetValues, One2OneDepResponse<DV, T>> getFromFacets,
      BiConsumer<FacetValues, One2OneDepResponse<DV, T>> setToFacets) {
    super(
        id,
        name,
        vajramID,
        type,
        ofVajram,
        onVajram,
        onVajramId,
        documentation,
        isBatched,
        tagsParser,
        getFromFacets,
        setToFacets);
  }
}
