package com.flipkart.krystal.traits;

import com.flipkart.krystal.core.VajramID;

public abstract sealed class DynamicDispatchPolicy implements TraitDispatchPolicy
    permits PredicateDispatchPolicy, ComputeDispatchPolicy {

  protected void validateDispatchTarget(VajramID dispatchTargetID) {
    if (!dispatchTargetIDs().contains(dispatchTargetID)) {
      throw new IllegalStateException(
          "Computed dispatch target "
              + dispatchTargetID
              + " is not present in dispatchTargetReqs. This is a configuration error!"
              + " Please update the dispatchTargetReqs to contain all possible dispatch targets.");
    }
  }
}
