package com.flipkart.krystal.krystex.decorators.resilience4j;

import static io.github.resilience4j.decorators.Decorators.ofCompletionStage;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.MainLogic;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.resilience4j.decorators.Decorators.DecorateCompletionStage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

final class R4JUtils {

  static DecorateCompletionStage<ImmutableMap<Inputs, CompletableFuture<Object>>>
      decorateAsyncExecute(MainLogic<Object> logicToDecorate, ImmutableList<Inputs> inputsList) {
    return ofCompletionStage(
        () -> {
          ImmutableMap<Inputs, CompletableFuture<Object>> result =
              logicToDecorate.execute(inputsList);
          return allOf(result.values().toArray(CompletableFuture[]::new))
              .handle((unused, throwable) -> result);
        });
  }

  static ImmutableMap<Inputs, CompletableFuture<Object>> extractResponseMap(
      ImmutableList<Inputs> inputsList,
      CompletionStage<ImmutableMap<Inputs, CompletableFuture<Object>>> decoratedCompletion) {
    //noinspection UnstableApiUsage
    ImmutableMap.Builder<Inputs, CompletableFuture<Object>> result =
        ImmutableMap.builderWithExpectedSize(inputsList.size());
    for (Inputs inputs : inputsList) {
      result.put(
          inputs,
          decoratedCompletion
              .thenApply(resultMap -> resultMap.get(inputs))
              .toCompletableFuture()
              .thenCompose(identity()));
    }
    return result.build();
  }

  private R4JUtils() {}
}
