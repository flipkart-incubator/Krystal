package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/** Represents the results of a depdendency invocation. */
public sealed interface Responses<R extends Request<T>, T> extends FacetValue<T> permits Results {

  ImmutableList<RequestResponse<R, T>> requestResponses();

  ImmutableMap<R, Errable<T>> asMap();

  static <R extends Request<T>, T> Responses<R, T> empty() {
    return Results.empty();
  }
}
