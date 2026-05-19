package com.flipkart.krystal.data;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.util.List;

/** Represents the results of a vajram invocation. */
public record RequestResponse<R extends Request<T>, T>(R request, Errable<T> response)
    implements One2OneDepResponse<R, T> {
  public static <R extends Request<T>, T> List<RequestResponse<R, T>> fromCompletedReqRespFutures(
      List<? extends RequestResponseFuture<? extends R, T>> requestResponseFutures) {
    Function<? super RequestResponseFuture<? extends R, T>, RequestResponse<R, T>> transformer =
        rrf -> {
          R r = requireNonNull(rrf).request();
          @SuppressWarnings("RedundantCast") // For checker-framework
          Errable<T> now =
              (Errable<T>) rrf.response().handle(Errable::errableFrom).getNow(Errable.nil());
          return new RequestResponse<>(r, now);
        };
    @SuppressWarnings("type.argument")
    List<RequestResponse<R, T>> transformedList =
        Lists.transform(requestResponseFutures, transformer);
    return transformedList;
  }
}
