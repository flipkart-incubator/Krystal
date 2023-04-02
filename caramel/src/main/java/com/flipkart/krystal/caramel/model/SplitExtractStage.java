package com.flipkart.krystal.caramel.model;

import static java.util.function.Function.identity;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public abstract class SplitExtractStage<INPUT, ROOT extends WorkflowPayload, OUTPUT> {
  public abstract <O_ITEM> SplitMergeStage<O_ITEM> transformEachResult(
      Function<OUTPUT, O_ITEM> extractor);

  public abstract <X> WorkflowBuildStage<INPUT, ROOT> collectTo(
      Field<X, ROOT> targetField, Function<? extends Collection<OUTPUT>, ? extends X> collector);

  public final <X> WorkflowBuildStage<INPUT, ROOT> collectTo(
      Field<? extends Collection<OUTPUT>, ROOT> targetField) {
    return collectTo(targetField, identity());
  }

  public abstract class SplitMergeStage<O_ITEM> {

    public abstract <X> WorkflowBuildStage<INPUT, ROOT> toCompute(
        Field<X, ROOT> sink, Function<List<O_ITEM>, ? extends X> merger);
  }
}
