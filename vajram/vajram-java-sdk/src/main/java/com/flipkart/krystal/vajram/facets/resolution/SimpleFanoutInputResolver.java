package com.flipkart.krystal.vajram.facets.resolution;

import static com.flipkart.krystal.facets.resolution.ResolverCommand.executeWithRequests;
import static com.flipkart.krystal.facets.resolution.ResolverCommand.skip;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil._resolutionHelper;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.ImmutableRequest.Builder;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.specs.DependencySpec;
import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.NonNull;

/** A resolver which resolves exactly one input of a dependency. */
public final class SimpleFanoutInputResolver<S, T, CV extends Request, DV extends Request>
    extends AbstractSimpleInputResolver<S, T, CV, DV> implements FanoutInputResolver {

  SimpleFanoutInputResolver(
      DependencySpec<?, CV, DV> dependency, SimpleInputResolverSpec<T, CV, DV> resolverSpec) {
    super(dependency, resolverSpec, true);
  }

  @Override
  public ResolverCommand resolve(Builder depRequest, Facets facets) {
    {
      try {
        //noinspection unchecked,rawtypes
        DependencyCommand<T> depCommand =
            _resolutionHelper(
                getResolverSpec().sources(),
                getResolverSpec().transformer(),
                getResolverSpec().fanoutTransformer(),
                getResolverSpec().skipConditions(),
                facets);
        if (depCommand instanceof FanoutCommand<T>) {
          if (depCommand.shouldSkip()) {
            return skip(depCommand.doc(), depCommand.skipCause());
          } else {
            if (depCommand.inputs().size() == 1) {
              getResolverSpec().targetInput().setToRequest(depRequest, depCommand.inputs().get(0));
              return executeWithRequests(ImmutableList.of((Builder) depRequest));
            } else {
              return executeWithRequests(
                  depCommand.inputs().stream()
                      .<@NonNull Builder>map(
                          o -> {
                            Builder next = (Builder) depRequest._newCopy();
                            getResolverSpec().targetInput().setToRequest(next, o);
                            return next;
                          })
                      .collect(toImmutableList()));
            }
          }
        } else {
          throw new AssertionError("Fanout input resolver must return FanoutCommand command only");
        }
      } catch (Exception e) {
        return skip(
            String.format(
                "Got exception %s while executing the resolver of the dependency %s",
                e, getDependency().name()),
            e);
      }
    }
  }
}
