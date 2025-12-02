package com.flipkart.krystal.vajramexecutor.krystex.traits;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.traits.ComputeDispatchPolicy;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.traits.DispatchTargetComputing.DispatchTargetComputer;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ComputeDispatchPolicyImpl<T extends Request<?>> extends ComputeDispatchPolicy {

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
    @Nullable Object dispatchTarget =
        dispatchTargetComputer.computeDispatchTarget(dependency, (T) request);
    if (dispatchTarget == null) {
      return null;
    }
    VajramID vajramID;
    Class<? extends Request<?>> dispatchTargetReqClass;
    if (dispatchTarget instanceof VajramID) {
      vajramID = (VajramID) dispatchTarget;
      dispatchTargetReqClass = graph.getVajramReqByVajramId(vajramID).orElse(null);
    } else if (dispatchTarget instanceof Class<?>) {
      dispatchTargetReqClass = (Class<? extends Request<?>>) dispatchTarget;
      vajramID = graph.getVajramIdByVajramReqType(dispatchTargetReqClass);
    } else {
      throw new AssertionError(
          "Dispatch target will be either a VajramID or a Class<? extends Request<?>>. This is a bug in platform code");
    }

    if (dispatchTargetReqClass == null) {
      throw new IllegalArgumentException(
          "Could not find request type for vajram Id: "
              + vajramID
              + ". Please check if the vajram has been loaded into the vajram graph");
    }
    if (!traitReqType.isAssignableFrom(dispatchTargetReqClass)) {
      throw new IllegalArgumentException(
          "Dispatch target id: "
              + vajramID
              + " does not implement the trait "
              + traitID
              + ". Please check your dispatch policy logic.");
    }
    return vajramID;
  }

  public ImmutableSet<Class<? extends Request<?>>> dispatchTargetReqs() {
    return ImmutableSet.copyOf(dispatchTargetReqs);
  }
}
