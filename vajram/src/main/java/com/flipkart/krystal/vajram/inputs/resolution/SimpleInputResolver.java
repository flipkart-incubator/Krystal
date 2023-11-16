package com.flipkart.krystal.vajram.inputs.resolution;

import static com.flipkart.krystal.data.ValueOrError.withValue;
import static com.flipkart.krystal.vajram.inputs.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajram.inputs.MultiExecute.skipFanout;
import static com.flipkart.krystal.vajram.inputs.SingleExecute.executeWith;
import static com.flipkart.krystal.vajram.inputs.SingleExecute.skipExecution;
import static com.flipkart.krystal.vajram.inputs.resolution.InputResolverUtil._resolutionHelper;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Optional.ofNullable;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.QualifiedInputs;
import com.flipkart.krystal.vajram.inputs.SingleExecute;
import com.flipkart.krystal.vajram.inputs.VajramDependencySpec;
import com.flipkart.krystal.vajram.inputs.VajramFacetSpec;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

/** A resolver which resolves exactly one input of a dependency. */
public final class SimpleInputResolver<S, T, CV extends Vajram<?>> extends AbstractInputResolver {
  public static final LongAdder TIME = new LongAdder();
  private final VajramDependencySpec<?, CV> dependency;
  private final SimpleInputResolverSpec<S, T> resolverSpec;

  SimpleInputResolver(
      VajramDependencySpec<?, CV> dependency, SimpleInputResolverSpec<S, T> resolverSpec) {
    super(
        ofNullable(resolverSpec.getSourceInput()).stream()
            .map(VajramFacetSpec::name)
            .collect(toImmutableSet()),
        new QualifiedInputs(dependency.name(), resolverSpec.getTargetInput().name()));
    this.dependency = dependency;
    this.resolverSpec = resolverSpec;
  }

  public VajramDependencySpec<?, ?> getDependency() {
    return dependency;
  }

  public SimpleInputResolverSpec<?, ?> getResolverSpec() {
    return resolverSpec;
  }

  /**
   * @see InputResolverUtil#multiResolve(List, Map, Inputs)
   */
  @Override
  public DependencyCommand<Inputs> resolve(
      String dependencyName, ImmutableSet<String> inputsToResolve, Inputs inputs) {
    long start = System.nanoTime();
    try {

      DependencyCommand<Object> depCommand =
          _resolutionHelper(
              resolverSpec.getSourceInput(),
              resolverSpec.getTransformer(),
              resolverSpec.getFanoutTransformer(),
              resolverSpec.getSkipConditions(),
              inputs);
      boolean shouldSkip = depCommand.shouldSkip();
      if (depCommand instanceof SingleExecute<Object> singleExecute) {
        if (shouldSkip) {
          return skipExecution(singleExecute.doc());
        } else {
          return executeWith(
              new Inputs(
                  ImmutableMap.of(
                      resolverSpec.getTargetInput().name(), withValue(singleExecute.input()))));
        }
      } else {
        if (shouldSkip) {
          return skipFanout(depCommand.doc());
        } else
          return executeFanoutWith(
              depCommand.inputs().stream()
                  .map(
                      o ->
                          new Inputs(
                              ImmutableMap.of(
                                  resolverSpec.getTargetInput().name(),
                                  new ValueOrError<>(o, Optional.empty()))))
                  .toList());
      }
    } catch (Exception e) {
      return skipExecution(
          String.format(
              "Got exception %s while executing the resolver of the dependency %s",
              e, dependency.name()));
    } finally {
      TIME.add(System.nanoTime() - start);
    }
  }
}
