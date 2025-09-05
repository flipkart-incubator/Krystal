package com.flipkart.krystal.vajramexecutor.krystex.traits;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.traits.DispatchCase;
import com.flipkart.krystal.traits.PredicateDynamicDispatchPolicy;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;

public final class PredicateDispatchPolicyImpl implements PredicateDynamicDispatchPolicy {

  @Getter private final VajramID traitID;
  @Getter private final ImmutableList<DispatchCase> dispatchCases;
  @Getter private final ImmutableList<VajramID> dispatchTargets;
  @Getter private final ImmutableList<Class<? extends Request<?>>> dispatchTargetReqs;

  public PredicateDispatchPolicyImpl(
      Class<? extends Request<?>> traitReqType,
      ImmutableList<DispatchCase> traitDispatchCases,
      VajramKryonGraph graph) {
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
}
