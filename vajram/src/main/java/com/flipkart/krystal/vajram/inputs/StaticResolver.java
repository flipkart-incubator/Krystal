package com.flipkart.krystal.vajram.inputs;

import lombok.Builder;

@Builder
public record StaticResolver(Object as, String dependencyName, String targetInputName) implements InputResolver {
  public static StaticResolver.StaticResolverBuilder staticResolve(String dep, String target){
    return builder().dependencyName(dep).targetInputName(target);
  }

  @Override
  public QualifiedInputId resolutionTarget() {
    return new QualifiedInputId(dependencyName(), null, targetInputName());
  }
}
