package com.flipkart.krystal.krystex.logicdecorators.resilience4j;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.function.Function.identity;

import com.flipkart.krystal.core.OutputLogicExecutionInput;
import com.flipkart.krystal.core.OutputLogicExecutionResults;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.krystex.OutputLogic;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.decorators.Decorators.DecorateCompletionStage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.checkerframework.checker.nullness.qual.Nullable;

final class R4JUtils {

  @SuppressWarnings("RedundantTypeArguments") // To avoid nullChecker errors
  static DecorateCompletionStage<OutputLogicExecutionResults<Object>> decorateAsyncExecute(
      OutputLogic<Object> logicToDecorate, OutputLogicExecutionInput input) {
    return Decorators.ofCompletionStage(
        () -> {
          OutputLogicExecutionResults<Object> executionResult = logicToDecorate.execute(input);
          var result = executionResult.results();
          return allOf(result.values().toArray(CompletableFuture[]::new))
              .handle((unused, throwable) -> executionResult);
        });
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  static OutputLogicExecutionResults<Object> extractResponseMap(
      ImmutableList<? extends FacetValues> facetsList,
      CompletionStage<OutputLogicExecutionResults<Object>> decoratedCompletion) {
    ImmutableMap.Builder<FacetValues, CompletableFuture<@Nullable Object>> result =
        ImmutableMap.builderWithExpectedSize(facetsList.size());
    for (FacetValues facetValues : facetsList) {
      //noinspection RedundantTypeArguments : Excplicit types to avoid NullChecker errors
      result.put(
          facetValues,
          decoratedCompletion
              .<CompletableFuture<@Nullable Object>>thenApply(
                  resultMap ->
                      checkNotNull(
                          resultMap.results().get(facetValues),
                          "No future found for inputs " + facetValues))
              .toCompletableFuture()
              .thenCompose(identity()));
    }
    return new OutputLogicExecutionResults<>(result.build());
  }

  private R4JUtils() {}
}
