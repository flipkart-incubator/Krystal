package com.flipkart.krystal.vajramexecutor.krystex.traits;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.traits.DispatchCase;
import com.flipkart.krystal.traits.PredicateDispatchPolicy;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class PredicateDispatchPolicyImpl extends PredicateDispatchPolicy {

  @Getter private final VajramID traitID;
  @Getter private final ImmutableList<DispatchCase> dispatchCases;
  @Getter private final ImmutableSet<VajramID> dispatchTargetIDs;
  @Getter private final ImmutableSet<Class<? extends Request<?>>> dispatchTargetReqs;
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
            .collect(toImmutableSet());
    this.dispatchTargetIDs =
        dispatchTargetReqs.stream()
            .map(graph::getVajramIdByVajramReqType)
            .collect(toImmutableSet());
  }

  @Override
  protected VajramID getVajramIdByVajramReqType(Class<? extends Request<?>> reqType) {
    return graph.getVajramIdByVajramReqType(reqType);
  }

  /**
   * No-op for PredicateDispatchPolicyImpl. Since dispatchTargetsReqs are computed from dispatch
   * cases, no explicit validation is needed.
   */
  @Override
  protected void validateDispatchTarget(VajramID dispatchTargetID) {
    return;
  }
}
