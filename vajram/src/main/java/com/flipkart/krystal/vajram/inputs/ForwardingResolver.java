package com.flipkart.krystal.vajram.inputs;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import lombok.Builder;

@Builder
public record ForwardingResolver(
    String from, String dependencyName, String targetInputName, Function<?, ?> using)
    implements InputResolver {
  public static ForwardingResolverBuilder forwardResolve(
      String dependencyName, String targetInputName) {
    return builder().dependencyName(dependencyName).targetInputName(targetInputName);
  }

  @Override
  public ImmutableSet<String> sources() {
    return ImmutableSet.of(from);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Function<?, Collection<?>> transformationLogic() {
    return using.andThen(Collections::singleton);
  }

  @Override
  public QualifiedInputs resolutionTarget() {
    return new QualifiedInputs(dependencyName(), null, targetInputName());
  }
}
