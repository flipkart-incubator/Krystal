package com.flipkart.krystal.krystex;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.failedFuture;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record BatchResult<T>(CompletableFuture<ImmutableList<T>> future) implements Result {

  public BatchResult() {
    this(new CompletableFuture<>());
  }

  public static <T> BatchResult<T> from(List<SingleResult<T>> resultsForRequest) {
    //noinspection unchecked
    CompletableFuture<T>[] futures =
        resultsForRequest.stream().map(SingleResult::future).toArray(CompletableFuture[]::new);
    return new BatchResult<>(
        allOf(futures)
            .thenApply(
                unused ->
                    Arrays.stream(futures)
                        .map(completableFuture -> completableFuture.getNow(null))
                        .collect(toImmutableList())));
  }

  /**
   * Converts this batch result into a list of single results.
   *
   * @throws IllegalStateException if this BatchResult.future() is not done.
   */
  public ImmutableList<SingleResult<T>> toSingleResults() {
    if (!future().isDone()) {
      throw new IllegalStateException(
          "Cannot convert BatchResult which is not done into a list of single results");
    }
    if (future().isCompletedExceptionally()) {
      return ImmutableList.of(
          this.future()
              .handle((result, throwable) -> new SingleResult<T>(failedFuture(throwable)))
              .getNow(null));
    }
    return this.future().getNow(null).stream()
        .map(CompletableFuture::completedFuture)
        .map(SingleResult::new)
        .collect(toImmutableList());
  }
}
