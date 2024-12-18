package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableCollection;

/** Represents the results of a depdendency invocation. */
public sealed interface DependencyResponses<R extends Request<T>, T> extends FacetValue
    permits DepResponsesImpl {

  ImmutableCollection<RequestResponse<R, T>> requestResponsePairs();

  Errable<T> getForRequest(R request);

  static <R extends Request<T>, T> DependencyResponses<R, T> empty() {
    return DepResponsesImpl.empty();
  }
}
