package com.flipkart.krystal.vajramexecutor.krystex.traits;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.traits.DispatchCase;
import com.flipkart.krystal.traits.PredicateDispatchPolicy;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class PredicateDispatchPolicyImpl extends PredicateDispatchPolicy {

  @Getter private final VajramID traitID;
  @Getter private final ImmutableList<DispatchCase> dispatchCases;
  @Getter private final ImmutableList<VajramID> dispatchTargets;
  @Getter private final ImmutableList<Class<? extends Request<?>>> dispatchTargetReqs;
  @NonNull private final VajramKryonGraph graph;

  public PredicateDispatchPolicyImpl(
      Class<? extends Request<?>> traitReqType,
      ImmutableList<DispatchCase> traitDispatchCases,
      VajramKryonGraph graph) {
    this.graph = graph;
    this.traitID = graph.getVajramIdByVajramReqType(traitReqType);
    this.dispatchCases = traitDispatchCases;
    this.dispatchTargetReqs =
        dispatchCases.stream()
            .map(DispatchCase::dispatchTargets)
            .flatMap(Collection::stream)
            .collect(toImmutableList());
    this.dispatchTargets =
        dispatchTargetReqs.stream()
            .map(graph::getVajramIdByVajramReqType)
            .collect(toImmutableList());
  }

  @Override
  protected VajramID getVajramIdByVajramReqType(Class<? extends Request<?>> reqType) {
    return graph.getVajramIdByVajramReqType(reqType);
  }
}
