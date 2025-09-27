package com.flipkart.krystal.krystex.logicdecorators.resilience4j;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.allOf;

import com.flipkart.krystal.core.OutputLogicExecutionInput;
import com.flipkart.krystal.krystex.OutputLogic;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.decorators.Decorators.DecorateCompletionStage;

final class R4JUtils {

  @SuppressWarnings("RedundantTypeArguments") // To avoid nullChecker errors
  static DecorateCompletionStage<Void> decorateAsyncExecute(
      OutputLogic<Object> logicToDecorate, OutputLogicExecutionInput input) {
    return Decorators.ofCompletionStage(
        () -> {
          logicToDecorate.execute(input);
          return allOf(input.responseFutures()).handle((unused, throwable) -> null);
        });
  }

  private R4JUtils() {}
}
