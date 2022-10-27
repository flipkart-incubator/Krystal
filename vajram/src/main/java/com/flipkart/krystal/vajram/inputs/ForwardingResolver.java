package com.flipkart.krystal.vajram.inputs;

import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
public record ForwardingResolver(String from, String dependencyName, String targetInputName, Function<? ,?> using) implements InputResolver {
  public static ForwardingResolverBuilder forwardResolve(String dependencyName, String targetInputName){
    return builder().dependencyName(dependencyName).targetInputName(targetInputName);
  }

  @Override
  public QualifiedInputId resolutionTarget() {
    return new QualifiedInputId(dependencyName(), null, targetInputName());
  }
}
