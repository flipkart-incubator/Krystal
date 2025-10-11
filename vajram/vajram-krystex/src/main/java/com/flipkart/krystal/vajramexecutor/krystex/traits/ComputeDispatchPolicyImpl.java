package com.flipkart.krystal.vajramexecutor.krystex.traits;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.traits.ComputeDispatchPolicy;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ComputeDispatchPolicyImpl<T extends Request<?>> extends ComputeDispatchPolicy {

  @FunctionalInterface
  public interface DispatchTargetComputer<T extends Request<?>> {

    @Nullable Class<? extends T> computeDispatchTarget(Dependency dependency, T request);
  }

  private final VajramKryonGraph graph;
  private final Class<T> traitReqType;

  @Getter private final VajramID traitID;
  @Getter private final ImmutableCollection<VajramID> dispatchTargets;
  private final ImmutableList<Class<? extends T>> dispatchTargetReqs;

  private final DispatchTargetComputer<T> dispatchTargetComputer;

  public ComputeDispatchPolicyImpl(
      Class<T> traitReqType,
      DispatchTargetComputer<T> dispatchTargetComputer,
      ImmutableList<Class<? extends T>> dispatchTargetReqs,
      VajramKryonGraph graph) {
    this.traitReqType = traitReqType;
    this.graph = graph;
    this.traitID = graph.getVajramIdByVajramReqType(traitReqType);
    this.dispatchTargetComputer = dispatchTargetComputer;
    this.dispatchTargetReqs = dispatchTargetReqs;
    this.dispatchTargets =
        dispatchTargetReqs.stream()
            .map(graph::getVajramIdByVajramReqType)
            .collect(toImmutableList());
  }

  @SuppressWarnings("unchecked")
  @Override
  public @Nullable VajramID getDispatchTarget(Dependency dependency, Request<?> request) {
    try {
      return graph.getVajramIdByVajramReqType(
          dispatchTargetComputer.computeDispatchTarget(dependency, (T) request));
    } catch (ClassCastException e) {
      throw new AssertionError(
          "Request type "
              + request.getClass()
              + " which is not a sub type of trait request type: "
              + traitReqType
              + ". This should not be possible. There seems to be a bug in the platform",
          e);
    }
  }

  public ImmutableList<Class<? extends Request<?>>> dispatchTargetReqs() {
    return ImmutableList.copyOf(dispatchTargetReqs);
  }
}
