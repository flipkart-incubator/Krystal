package com.flipkart.krystal.data;

import com.flipkart.krystal.data.FacetValue.SingleFacetValue;

public sealed interface FacetValue<T> permits DepResponse, Errable, SingleFacetValue {
  sealed interface SingleFacetValue<T> extends FacetValue<T> permits Errable, One2OneDepResponse {
    Errable<T> asErrable();
  }
}
