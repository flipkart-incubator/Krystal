package com.flipkart.krystal.vajram.facets.resolution;

import static com.flipkart.krystal.data.Errable.withValue;
import static com.flipkart.krystal.resolution.ResolverCommand.skip;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil._resolutionHelper;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestBuilder;
import com.flipkart.krystal.resolution.ResolverCommand;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.SingleExecute;
import com.flipkart.krystal.vajram.facets.VajramDependencySpec;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** A resolver which resolves exactly one input of a dependency. */
public final class SimpleSingleInputResolver<S, T, CV extends Request<?>, DV extends Request<?>>
    extends SimpleInputResolver<S, T, CV, DV> implements SingleInputResolver {

  SimpleSingleInputResolver(
      VajramDependencySpec<?, ?, CV, DV> dependency,
      SimpleInputResolverSpec<T, CV, DV> resolverSpec) {
    super(dependency, resolverSpec);
  }

  @Override
  public ResolverCommand resolve(ImmutableList<RequestBuilder<Object>> depRequests, Facets facets) {
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
        if (depCommand instanceof SingleExecute<Object> singleExecute) {
          ResolverCommand command;
          if (depCommand.shouldSkip()) {
            command = skip(singleExecute.doc());
          } else {
            for (RequestBuilder<?> depRequest : depRequests) {
              depRequest._set(
                  getResolverSpec().targetInput().id(), withValue(singleExecute.input()));
            }
            command = ResolverCommand.computedRequests(depRequests);
          }
          return command;
        } else {
          throw new IllegalStateException(
              "single input resolver must return SingleExecute command only");
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
