package com.flipkart.krystal.caramel.model;

import java.util.function.Consumer;
import java.util.function.Function;

public interface TerminatedWorkflow<INPUT, ROOT extends WorkflowPayload, OUTPUT>
    extends WorkflowCompletionStage, Function<INPUT, OUTPUT>, Consumer<INPUT> {
  @Override
  default <V> Function<INPUT, V> andThen(Function<? super OUTPUT, ? extends V> after) {
    return new WorkflowPostProcessor<>() {
      @Override
      public V apply(INPUT input) {
        return null;
      }

      @Override
      public void accept(INPUT input) {}
    };
  }

  interface WorkflowPostProcessor<INPUT, ROOT extends WorkflowPayload, OUTPUT>
      extends TerminatedWorkflow<INPUT, ROOT, OUTPUT> {}
}
