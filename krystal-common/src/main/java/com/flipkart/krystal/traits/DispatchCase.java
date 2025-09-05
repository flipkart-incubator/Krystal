package com.flipkart.krystal.traits;

import com.flipkart.krystal.data.Request;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;

public interface DispatchCase {

  Optional<? extends Class<? extends Request<?>>> computeDispatchTarget(Request<?> request);

  ImmutableSet<Class<? extends Request<?>>> dispatchTargets();
}
