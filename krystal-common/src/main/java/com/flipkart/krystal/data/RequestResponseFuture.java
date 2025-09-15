package com.flipkart.krystal.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Represents the request and a response placeholder for a dependency invocation. */
public record RequestResponseFuture<R extends Request<T>, T>(
    R request, CompletableFuture<T> response) {
  public static <R extends Request<T>, T> RequestResponseFuture<R, T> forRequest(R request) {
    return new RequestResponseFuture<>(request, new CompletableFuture<>());
  }

  public static <R extends Request<T>, T> List<RequestResponseFuture<R, T>> forRequests(
      List<R> request) {
    List<RequestResponseFuture<R, T>> list = new ArrayList<>();
    for (R r : request) {
      list.add(new RequestResponseFuture<>(r, new CompletableFuture<>()));
    }
    return list;
  }

  public static <R extends Request<T>, T> CompletableFuture<T>[] getFutures(
      List<RequestResponseFuture<R, T>> requestResponseFutures) {
    @SuppressWarnings("unchecked")
    CompletableFuture<T>[] list =
        (CompletableFuture<T>[]) new CompletableFuture[requestResponseFutures.size()];
    for (int i = 0; i < requestResponseFutures.size(); i++) {
      list[i] = requestResponseFutures.get(i).response();
    }
    return list;
  }
}
