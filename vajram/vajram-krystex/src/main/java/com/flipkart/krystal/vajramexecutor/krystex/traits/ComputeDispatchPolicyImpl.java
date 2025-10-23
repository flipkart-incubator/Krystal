package com.flipkart.krystal.vajramexecutor.krystex.traits;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.traits.ComputeDispatchPolicy;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ComputeDispatchPolicyImpl<T extends Request<?>> extends ComputeDispatchPolicy {

  @FunctionalInterface
  public interface DispatchTargetComputer<T extends Request<?>> {

    @Nullable Class<? extends T> computeDispatchTarget(@Nullable Dependency dependency, T request);
  }

  private final VajramKryonGraph graph;
  private final Class<T> traitReqType;

  @Getter private final VajramID traitID;
  @Getter private final ImmutableSet<VajramID> dispatchTargetIDs;
  private final ImmutableSet<Class<? extends T>> dispatchTargetReqs;

  private final DispatchTargetComputer<T> dispatchTargetComputer;

  public ComputeDispatchPolicyImpl(
      Class<T> traitReqType,
      DispatchTargetComputer<T> dispatchTargetComputer,
      ImmutableSet<Class<? extends T>> dispatchTargetReqs,
      VajramKryonGraph graph) {
    this.traitReqType = traitReqType;
    this.graph = graph;
    this.traitID = graph.getVajramIdByVajramReqType(traitReqType);
    this.dispatchTargetComputer = dispatchTargetComputer;
    this.dispatchTargetReqs = dispatchTargetReqs;
    this.dispatchTargetIDs =
        dispatchTargetReqs.stream()
            .map(graph::getVajramIdByVajramReqType)
            .collect(toImmutableSet());
  }

  @SuppressWarnings("unchecked")
  @Override
  public @Nullable VajramID getDispatchTargetID(
      @Nullable Dependency dependency, Request<?> request) {
    try {
      Class<? extends T> dispatchTarget =
          dispatchTargetComputer.computeDispatchTarget(dependency, (T) request);
      if (dispatchTarget == null) {
        return null;
      }
      return graph.getVajramIdByVajramReqType(dispatchTarget);
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

  public ImmutableSet<Class<? extends Request<?>>> dispatchTargetReqs() {
    return ImmutableSet.copyOf(dispatchTargetReqs);
  }
}
