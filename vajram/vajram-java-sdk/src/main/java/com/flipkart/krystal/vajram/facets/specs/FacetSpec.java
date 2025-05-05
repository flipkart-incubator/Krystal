package com.flipkart.krystal.vajram.facets.specs;

import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.Facet;

public sealed interface FacetSpec<T, CV extends Request> extends Facet
    permits AbstractFacetSpec, MandatoryFacetSpec, OptionalFacetSpec {

  boolean isMandatoryOnServer();

  boolean isBatched();

  boolean canFanout();

  DataType<T> type();

  Class<CV> ofVajram();

  @Override
  FacetValue<T> getFacetValue(FacetValues facetValues);
}
