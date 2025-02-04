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
import com.flipkart.krystal.vajram.facets.MultiExecute;
import com.flipkart.krystal.vajram.facets.specs.DependencySpec;
import com.google.common.collect.ImmutableList;
import java.util.Iterator;
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
        if (depCommand instanceof MultiExecute<T>) {
          if (depCommand.shouldSkip()) {
            return skip(depCommand.doc(), depCommand.skipCause());
          } else {
            Iterator<Builder> newReqIterator =
                new Iterator<Builder>() {
                  private boolean first = true;

                  @Override
                  public Builder next() {
                    if (first) {
                      first = false;
                      return (Builder) depRequest;
                    } else {
                      return (Builder) depRequest._newCopy();
                    }
                  }

                  @Override
                  public boolean hasNext() {
                    return true;
                  }
                };
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
          throw new IllegalStateException(
              "Fanout input resolver must return MultiExecute command only");
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
