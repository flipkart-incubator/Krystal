package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableList;

/** Represents the results of a depdendency invocation. */
public sealed interface Responses<R extends Request<T>, T> extends FacetValue<T> permits Results {

  ImmutableList<Response<R, T>> responses();
}
