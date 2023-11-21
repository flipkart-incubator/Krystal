package com.flipkart.krystal.krystex.logicdecorators.resilience4j;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.MainLogic;
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
  static DecorateCompletionStage<ImmutableMap<Inputs, CompletableFuture<@Nullable Object>>>
      decorateAsyncExecute(MainLogic<Object> logicToDecorate, ImmutableList<Inputs> inputsList) {
    return Decorators.<ImmutableMap<Inputs, CompletableFuture<@Nullable Object>>>ofCompletionStage(
        () -> {
          var result = logicToDecorate.execute(inputsList);
          return allOf(result.values().toArray(CompletableFuture[]::new))
              .<ImmutableMap<Inputs, CompletableFuture<@Nullable Object>>>handle(
                  (unused, throwable) -> result);
        });
  }

  static ImmutableMap<Inputs, CompletableFuture<@Nullable Object>> extractResponseMap(
      ImmutableList<Inputs> inputsList,
      CompletionStage<ImmutableMap<Inputs, CompletableFuture<@Nullable Object>>>
          decoratedCompletion) {
    //noinspection UnstableApiUsage
    ImmutableMap.Builder<Inputs, CompletableFuture<@Nullable Object>> result =
        ImmutableMap.builderWithExpectedSize(inputsList.size());
    for (Inputs inputs : inputsList) {
      //noinspection RedundantTypeArguments : Excplicit types to avoid NullChecker errors
      result.put(
          inputs,
          decoratedCompletion
              .<CompletableFuture<@Nullable Object>>thenApply(
                  resultMap -> {
                    return Optional.ofNullable(resultMap.get(inputs))
                        .orElseThrow(
                            () ->
                                new IllegalStateException("No future found for inputs " + inputs));
                  })
              .toCompletableFuture()
              .thenCompose(identity()));
    }
    return result.build();
  }

  private R4JUtils() {}
}
