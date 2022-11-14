package com.flipkart.krystal.caramel.model;

import static java.util.function.Function.identity;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public abstract class SplitStage<INPUT, ROOT extends WorkflowPayload, I_ITEM> {
  public abstract SplitStage<INPUT, ROOT, I_ITEM> stopOnException();

  public abstract SplitStage<INPUT, ROOT, I_ITEM> sequentially();

  public abstract <OUTPUT, SUB extends WorkflowPayload> SplitExtractStage<OUTPUT> processEachWith(
      TerminatedWorkflow<I_ITEM, SUB, OUTPUT> subWorkflow);

  public abstract class SplitExtractStage<OUTPUT> {
    public abstract <O_ITEM> SplitMergeStage<O_ITEM> extractEachWith(
        Function<OUTPUT, O_ITEM> extractor);

    public abstract <X> WorkflowBuildStage<INPUT, ROOT> toCompute(
        Field<X, ROOT> sink, Function<? extends List<OUTPUT>, ? extends X> merger);

    public final <X> WorkflowBuildStage<INPUT, ROOT> toCompute(
        Field<? extends Collection<OUTPUT>, ROOT> sink) {
      return toCompute(sink, identity());
    }

    public abstract class SplitMergeStage<O_ITEM> {

      public abstract <X> WorkflowBuildStage<INPUT, ROOT> toCompute(
          Field<X, ROOT> sink, Function<List<O_ITEM>, ? extends X> merger);
    }
  }
}
