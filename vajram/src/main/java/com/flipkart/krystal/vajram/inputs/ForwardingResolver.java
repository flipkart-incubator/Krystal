package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.data.Inputs;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import lombok.Builder;

@Builder
record ForwardingResolver(
    String from, String dependencyName, String targetInputName, Function<Object, Object> using)
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
  public Function<Object, Collection<?>> transformationLogic() {
    return using.andThen(Collections::singleton);
  }

  @Override
  public QualifiedInputs resolutionTarget() {
    return new QualifiedInputs(dependencyName(), null, targetInputName());
  }

  @Override
  public DependencyCommand<Inputs> resolve(
      String dependencyName, ImmutableSet<String> inputsToResolve, Inputs inputs) {
    return DependencyCommand.singleExecuteWith(
        new Inputs(ImmutableMap.of(targetInputName(), inputs.getInputValue(this.from()))));
  }
}
