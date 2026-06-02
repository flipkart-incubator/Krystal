package com.flipkart.krystal.traits;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.google.common.collect.ImmutableSet;
import java.lang.annotation.Annotation;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SimpleStaticDispatchPolicy extends StaticDispatchPolicy {

  private final VajramID traitId;
  private final VajramID dispatchTarget;
  private final Class<? extends Request<?>> dispatchTargetReq;

  public SimpleStaticDispatchPolicy(
      VajramID traitId, VajramID dispatchTarget, Class<? extends Request<?>> dispatchTargetReq) {
    this.traitId = traitId;
    this.dispatchTarget = dispatchTarget;
    this.dispatchTargetReq = dispatchTargetReq;
  }

  @Override
  public VajramID getDispatchTargetID(Dependency dependency) {
    return dispatchTarget;
  }

  @Override
  public Class<? extends Request<?>> getDispatchTarget(Dependency dependency) {
    return dispatchTargetReq;
  }

  @Override
  public VajramID getDispatchTargetID(@Nullable Annotation qualifier) {
    if (qualifier == null) {
      return dispatchTarget;
    } else {
      throw new IllegalArgumentException("Qualifier not supported");
    }
  }

  @Override
  public Class<? extends Request<?>> getDispatchTarget(@Nullable Annotation qualifier) {
    if (qualifier == null) {
      return dispatchTargetReq;
    } else {
      throw new IllegalArgumentException("Qualifier not supported");
    }
  }

  @Override
  public VajramID traitID() {
    return traitId;
  }

  @Override
  public ImmutableSet<VajramID> dispatchTargetIDs() {
    return ImmutableSet.of(dispatchTarget);
  }

  @Override
  public ImmutableSet<Class<? extends Request<?>>> dispatchTargetReqs() {
    return ImmutableSet.of(dispatchTargetReq);
  }
}
