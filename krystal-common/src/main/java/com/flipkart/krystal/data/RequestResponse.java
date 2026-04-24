package com.flipkart.krystal.data;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Lists;
import java.util.List;

/** Represents the results of a vajram invocation. */
public record RequestResponse<R extends Request<T>, T>(R request, Errable<T> response)
    implements One2OneDepResponse<R, T> {
  public static <R extends Request<T>, T> List<RequestResponse<R, T>> fromCompletedReqRespFutures(
      List<? extends RequestResponseFuture<? extends R, T>> requestResponseFutures) {
    return Lists.transform(
        requestResponseFutures,
        rrf ->
            new RequestResponse<>(
                requireNonNull(rrf).request(),
                rrf.response().handle(Errable::errableFrom).getNow(Errable.nil())));
  }
}
