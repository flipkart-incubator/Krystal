package com.flipkart.krystal.krystex.logicdecorators.resilience4j;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.OutputLogic;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.decorators.Decorators.DecorateCompletionStage;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.checkerframework.checker.nullness.qual.Nullable;

final class R4JUtils {

  @SuppressWarnings("RedundantTypeArguments") // To avoid nullChecker errors
  static DecorateCompletionStage<ImmutableMap<Facets, CompletableFuture<@Nullable Object>>>
      decorateAsyncExecute(OutputLogic<Object> logicToDecorate, ImmutableList<Facets> facetsList) {
    return Decorators.<ImmutableMap<Facets, CompletableFuture<@Nullable Object>>>ofCompletionStage(
        () -> {
          var result = logicToDecorate.execute(facetsList);
          return allOf(result.values().toArray(CompletableFuture[]::new))
              .<ImmutableMap<Facets, CompletableFuture<@Nullable Object>>>handle(
                  (unused, throwable) -> result);
        });
  }

  static ImmutableMap<Facets, CompletableFuture<@Nullable Object>> extractResponseMap(
      ImmutableList<Facets> facetsList,
      CompletionStage<ImmutableMap<Facets, CompletableFuture<@Nullable Object>>>
          decoratedCompletion) {
    //noinspection UnstableApiUsage
    ImmutableMap.Builder<Facets, CompletableFuture<@Nullable Object>> result =
        ImmutableMap.builderWithExpectedSize(facetsList.size());
    for (Facets facets : facetsList) {
      //noinspection RedundantTypeArguments : Excplicit types to avoid NullChecker errors
      result.put(
          facets,
          decoratedCompletion
              .<CompletableFuture<@Nullable Object>>thenApply(
                  resultMap -> {
                    return Optional.ofNullable(resultMap.get(facets))
                        .orElseThrow(
                            () ->
                                new IllegalStateException("No future found for inputs " + facets));
                  })
              .toCompletableFuture()
              .thenCompose(identity()));
    }
    return result.build();
  }

  private R4JUtils() {}
}
