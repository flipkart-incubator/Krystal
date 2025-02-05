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

public final class MandatoryOne2OneDepSpec<T, CV extends Request, DV extends Request>
    extends One2OneDepSpec<T, CV, DV> implements MandatoryFacetSpec<T, CV> {

  public MandatoryOne2OneDepSpec(
      int id,
      String name,
      DataType<T> type,
      Class<CV> ofVajram,
      Class<DV> onVajram,
      VajramID onVajramId,
      String documentation,
      boolean isBatched,
      ElementTags tags,
      Function<FacetValues, One2OneDepResponse> getFromFacets,
      BiConsumer<FacetValues, RequestResponse> setToFacets) {
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
