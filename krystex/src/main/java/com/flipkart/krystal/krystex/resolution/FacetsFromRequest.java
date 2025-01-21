package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.data.FacetsBuilder;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.krystex.Logic;

@FunctionalInterface
public non-sealed interface FacetsFromRequest extends Logic {
  FacetsBuilder facetsFromRequest(Request requestBuilder);
}
