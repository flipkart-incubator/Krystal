package com.flipkart.krystal.caramel.model;

import java.util.function.Function;

public abstract class ForkStage<INPUT, ROOT extends WorkflowPayload> {
  public abstract <I> ForkedWorkflowStage<I> withInput(CaramelField<I, ROOT> field);

  public abstract class ForkedWorkflowStage<SUB_I> {
    public abstract <O, SUB_P extends WorkflowPayload> ForkOutputStage<SUB_P> usingWorkflow(
        TerminatedWorkflow<SUB_I, SUB_P, O> subWorkflow);

    public abstract class ForkOutputStage<SUB_P extends WorkflowPayload> {

      public abstract <O, S> WorkflowBuildStage<INPUT, ROOT> toCompute(
          CaramelField<O, ROOT> sink, Function<S, O> outputExtractor);
    }
  }
}
