package com.flipkart.krystal.vajram.inputs;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

public sealed interface InputResolver extends InputResolverDefinition
    permits ForwardingResolver, ForwardingResolverV2, StaticResolver {

  default <S, T> Function<S, Collection<T>> transformationLogic() {
    return s -> Collections.emptyList();
  }
}
