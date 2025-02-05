package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.data.FacetValuesBuilder;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.krystex.Logic;

@FunctionalInterface
public non-sealed interface FacetsFromRequest extends Logic {
  FacetValuesBuilder facetsFromRequest(Request requestBuilder);
}
