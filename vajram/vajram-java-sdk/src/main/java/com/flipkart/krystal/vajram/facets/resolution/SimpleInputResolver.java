package com.flipkart.krystal.vajram.facets.resolution;

import static com.flipkart.krystal.data.Errable.withValue;
import static com.flipkart.krystal.vajram.facets.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.MultiExecute.skipFanout;
import static com.flipkart.krystal.vajram.facets.SingleExecute.skipExecution;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil._resolutionHelper;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestBuilder;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.QualifiedInputs;
import com.flipkart.krystal.vajram.facets.SingleExecute;
import com.flipkart.krystal.vajram.facets.VajramDependencySpec;
import com.flipkart.krystal.vajram.facets.VajramFacetSpec;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/** A resolver which resolves exactly one input of a dependency. */
public final class SimpleInputResolver<
        S, T, CV extends ImmutableRequest<?>, DV extends ImmutableRequest<?>>
    extends AbstractInputResolver {
  public static final LongAdder TIME = new LongAdder();
  private final VajramDependencySpec<?, ?, CV, DV> dependency;
  private final SimpleInputResolverSpec<T, CV, DV> resolverSpec;

  SimpleInputResolver(
      VajramDependencySpec<?, ?, CV, DV> dependency,
      SimpleInputResolverSpec<T, CV, DV> resolverSpec) {
    super(
        resolverSpec.sourceInputs().stream().map(VajramFacetSpec::id).collect(toImmutableSet()),
        new QualifiedInputs(dependency.id(), resolverSpec.targetInput().name()));
    this.dependency = dependency;
    this.resolverSpec = resolverSpec;
  }

  public VajramDependencySpec<?, ?, ?, ?> getDependency() {
    return dependency;
  }

  public SimpleInputResolverSpec<?, ?, ?> getResolverSpec() {
    return resolverSpec;
  }

  /**
   * @see InputResolverUtil#multiResolve(List, Map, Facets)
   */
  @Override
  public DependencyCommand<? extends Request<Object>> resolve(
      ImmutableList<? extends RequestBuilder<Object>> depRequests, Facets facets) {
    long start = System.nanoTime();
    try {
      //noinspection unchecked,rawtypes
      DependencyCommand<Object> depCommand =
          _resolutionHelper(
              (List) resolverSpec.sourceInputs(),
              resolverSpec.transformer(),
              resolverSpec.fanoutTransformer(),
              resolverSpec.skipConditions(),
              facets);
      boolean shouldSkip = depCommand.shouldSkip();
      if (depCommand instanceof SingleExecute<Object> singleExecute) {
        if (shouldSkip) {
          return skipExecution(singleExecute.doc());
        } else {
          for (RequestBuilder<?> depRequest : depRequests) {
            depRequest._set(resolverSpec.targetInput().id(), withValue(singleExecute.input()));
          }
          return executeFanoutWith(depRequests);
        }
      } else {
        if (shouldSkip) {
          return skipFanout(depCommand.doc());
        } else {
          RequestBuilder<Object> requestBuilder = depRequests.iterator().next()._newCopy();
          return executeFanoutWith(
              depCommand.inputs().stream()
                  .map(o -> requestBuilder._set(resolverSpec.targetInput().id(), withValue(o)))
                  .collect(toImmutableList()));
        }
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
