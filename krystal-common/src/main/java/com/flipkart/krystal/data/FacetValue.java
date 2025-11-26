package com.flipkart.krystal.data;

import com.flipkart.krystal.data.FacetValue.SingleFacetValue;

public sealed interface FacetValue<T> permits DepResponse, SingleFacetValue {
  sealed interface SingleFacetValue<T> extends FacetValue<T>
      permits ErrableFacetValue, One2OneDepResponse {
    Errable<T> asErrable();
  }
}
