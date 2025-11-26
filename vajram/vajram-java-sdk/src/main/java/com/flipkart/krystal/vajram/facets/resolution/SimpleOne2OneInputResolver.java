package com.flipkart.krystal.vajram.facets.resolution;

import static com.flipkart.krystal.facets.resolution.ResolverCommand.executeWithRequests;
import static com.flipkart.krystal.facets.resolution.ResolverCommand.skip;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil._resolutionHelper;

import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.ImmutableRequest.Builder;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.One2OneCommand;
import com.flipkart.krystal.vajram.facets.specs.DependencySpec;
import java.util.List;

/** A resolver which resolves exactly one input of a dependency. */
public final class SimpleOne2OneInputResolver<S, T, CV extends Request<?>, DV extends Request<?>>
    extends AbstractSimpleInputResolver<S, T, CV, DV> implements One2OneInputResolver {

  SimpleOne2OneInputResolver(
      DependencySpec<?, CV, DV> dependency, SimpleInputResolverSpec<T, CV, DV> resolverSpec) {
    super(dependency, resolverSpec, false);
  }

  @Override
  public ResolverCommand resolve(
      List<? extends Builder<?>> _depRequests, FacetValues _rawFacetValues) {
    {
      try {
        //noinspection unchecked,rawtypes
        DependencyCommand<T> depCommand =
            _resolutionHelper(
                getResolverSpec().source(),
                getResolverSpec().transformer(),
                getResolverSpec().skipConditions(),
                _rawFacetValues);
        if (depCommand instanceof One2OneCommand<T> one2OneCommand) {
          ResolverCommand command;
          if (depCommand.shouldSkip()) {
            command = skip(one2OneCommand.doc(), one2OneCommand.skipCause());
          } else {
            for (ImmutableRequest.Builder depRequest : _depRequests) {
              getResolverSpec().targetInput().setToRequest(depRequest, one2OneCommand.input());
            }
            command = executeWithRequests(_depRequests);
          }
          return command;
        } else {
          throw new IllegalStateException(
              "single input resolver must return One2OneCommand command only");
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
