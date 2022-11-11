package com.flipkart.krystal.vajram.inputs;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

public sealed interface InputResolver
    permits ForwardingResolver, ForwardingResolverV2, StaticResolver, DefaultInputResolver {

  ImmutableSet<String> sources();

  default <S, T> Function<S, Collection<T>> transformationLogic() {
    return s -> Collections.emptyList();
  }

  QualifiedInputs resolutionTarget();
}
