package com.flipkart.krystal.vajram.inputs;

import com.google.common.collect.ImmutableSet;
import lombok.Builder;

@Builder
public record StaticResolver(Object as, String dependencyName, String targetInputName)
    implements InputResolver {
  public static StaticResolver.StaticResolverBuilder staticResolve(String dep, String target) {
    return builder().dependencyName(dep).targetInputName(target);
  }

  @Override
  public ImmutableSet<String> sources() {
    return ImmutableSet.of();
  }

  @Override
  public QualifiedInputs resolutionTarget() {
    return new QualifiedInputs(dependencyName(), null, targetInputName());
  }
}
