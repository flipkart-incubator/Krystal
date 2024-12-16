package com.flipkart.krystal.vajram.facets.resolution;

import static com.flipkart.krystal.data.Errable.withValue;
import static com.flipkart.krystal.resolution.ResolverCommand.executeWithRequests;
import static com.flipkart.krystal.resolution.ResolverCommand.skip;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil._resolutionHelper;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestBuilder;
import com.flipkart.krystal.resolution.ResolverCommand;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.MultiExecute;
import com.flipkart.krystal.vajram.facets.VajramDependencySpec;
import java.util.List;

/** A resolver which resolves exactly one input of a dependency. */
public final class SimpleFanoutInputResolver<S, T, CV extends Request<?>, DV extends Request<?>>
    extends AbstractSimpleInputResolver<S, T, CV, DV> implements FanoutInputResolver {

  SimpleFanoutInputResolver(
      VajramDependencySpec<?, ?, CV, DV> dependency,
      SimpleInputResolverSpec<T, CV, DV> resolverSpec) {
    super(dependency, resolverSpec);
  }

  @Override
  public ResolverCommand resolve(RequestBuilder<?> depRequest, Facets facets) {
    {
      long start = System.nanoTime();
      try {
        //noinspection unchecked,rawtypes
        DependencyCommand<Object> depCommand =
            _resolutionHelper(
                (List) getResolverSpec().sourceInputs(),
                getResolverSpec().transformer(),
                getResolverSpec().fanoutTransformer(),
                getResolverSpec().skipConditions(),
                facets);
        if (depCommand instanceof MultiExecute<Object>) {
          if (depCommand.shouldSkip()) {
            return skip(depCommand.doc());
          } else {
            return executeWithRequests(
                depCommand.inputs().stream()
                    .map(
                        o ->
                            depRequest
                                ._asBuilder()
                                ._set(getResolverSpec().targetInput().id(), withValue(o)))
                    .collect(toImmutableList()));
          }
        } else {
          throw new IllegalStateException(
              "Fanout input resolver must return MultiExecute command only");
        }
      } catch (Exception e) {
        return skip(
            String.format(
                "Got exception %s while executing the resolver of the dependency %s",
                e, getDependency().name()));
      } finally {
        TIME.add(System.nanoTime() - start);
      }
    }
  }
}
