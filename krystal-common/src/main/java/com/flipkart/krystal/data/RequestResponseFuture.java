package com.flipkart.krystal.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Represents the request and a response placeholder for a vajram invocation. */
@Slf4j
public record RequestResponseFuture<R extends Request<T>, T>(
    R request, CompletableFuture<@Nullable T> response) {
  @SuppressWarnings("Convert2Diamond") // Needed to prevent null-checker errors
  public static <R extends Request<T>, T> RequestResponseFuture<R, T> forRequest(R request) {
    return new RequestResponseFuture<R, T>(request, new CompletableFuture<@Nullable T>());
  }

  @SuppressWarnings("unchecked")
  public static <R extends Request<T>, T> List<RequestResponseFuture<R, T>> forRequests(
      List<R> requests) {
    List<RequestResponseFuture<R, T>> list = new ArrayList<>();
    for (R r : requests) {
      list.add(forRequest((R) r._build()));
    }
    return list;
  }

  @SuppressWarnings("unchecked")
  public static <R extends Request<T>, B extends R, T>
      List<RequestResponseFuture<R, T>> forRequestBuilders(List<B> requests) {
    List<RequestResponseFuture<R, T>> list = new ArrayList<>();
    for (B r : requests) {
      RequestResponseFuture<R, T> rrf = forRequestBuilder(r);
      list.add(rrf);
    }
    return list;
  }

  @SuppressWarnings("unchecked")
  public static <R extends Request<T>, B extends R, T>
      RequestResponseFuture<B, T> forRequestBuilder(B builder) {
    return new RequestResponseFuture<>(builder, new CompletableFuture<@Nullable T>());
  }

  public static <R extends Request<T>, T> CompletableFuture<@Nullable T>[] getFutures(
      List<RequestResponseFuture<R, T>> requestResponseFutures) {
    @SuppressWarnings("unchecked")
    CompletableFuture<@Nullable T>[] array =
        (CompletableFuture<@Nullable T>[]) new CompletableFuture[requestResponseFutures.size()];
    for (int i = 0; i < requestResponseFutures.size(); i++) {
      array[i] = requestResponseFutures.get(i).response();
    }
    return array;
  }
}
