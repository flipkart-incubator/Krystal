package com.flipkart.krystal.vajram.facets.resolution;

import static com.flipkart.krystal.facets.resolution.ResolverCommand.skip;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil._resolutionHelper;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.ImmutableRequest.Builder;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.SingleExecute;
import com.flipkart.krystal.vajram.facets.specs.DependencySpec;
import com.google.common.collect.ImmutableList;

/** A resolver which resolves exactly one input of a dependency. */
public final class SimpleOne2OneInputResolver<S, T, CV extends Request, DV extends Request>
    extends AbstractSimpleInputResolver<S, T, CV, DV> implements One2OneInputResolver {

  SimpleOne2OneInputResolver(
      DependencySpec<?, CV, DV> dependency, SimpleInputResolverSpec<T, CV, DV> resolverSpec) {
    super(dependency, resolverSpec, false);
  }

  @Override
  public ResolverCommand resolve(ImmutableList<? extends Builder> depRequests, Facets facets) {
    {
      long start = System.nanoTime();
      try {
        //noinspection unchecked,rawtypes
        DependencyCommand<T> depCommand =
            _resolutionHelper(
                getResolverSpec().sources(),
                getResolverSpec().transformer(),
                getResolverSpec().fanoutTransformer(),
                getResolverSpec().skipConditions(),
                facets);
        if (depCommand instanceof SingleExecute<T> singleExecute) {
          ResolverCommand command;
          if (depCommand.shouldSkip()) {
            command = skip(singleExecute.doc(), singleExecute.skipCause());
          } else {
            for (Builder depRequest : depRequests) {
              getResolverSpec().targetInput().setToRequest(depRequest, singleExecute.input());
            }
            command = ResolverCommand.executeWithRequests(depRequests);
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
                e, getDependency().name()),
            e);
      } finally {
        TIME.add(System.nanoTime() - start);
      }
    }
  }
}
