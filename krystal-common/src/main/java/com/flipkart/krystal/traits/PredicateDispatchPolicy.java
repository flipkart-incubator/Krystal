package com.flipkart.krystal.traits;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.traits.matchers.InputValueMatcher;
import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link TraitDispatchPolicy} which allows trait invocations to be dispatched to conformant
 * Vajrams using pattern matching, allowing Krystal to support <a
 * href="https://en.wikipedia.org/wiki/Dynamic_dispatch">dynamic</a> <a
 * href="https://en.wikipedia.org/wiki/Predicate_dispatch">predicate</a> <a
 * href="https://en.wikipedia.org/wiki/Multiple_dispatch">multiple</a> dispatch
 *
 * <p>An object of this class contains patterns matchers which are mapped to inputs.The trait inputs
 * which are tagged as {@link UseForPredicateDispatch @UseForPredicateDispatch} are matched against
 * their corresponding pattern to determine which vajram to dispatch the invocation to.
 *
 * <p>Since full predicate dispatch can become extremely complex to understand and maintain, the set
 * of predicates supported is {@link InputValueMatcher limite and sealed} to prevent arbitrarily
 * complex predicates.
 *
 * @implNote Use predicate dispatch only when absolutely needed, and the problem cannot be solved by
 *     static dispatch. Static dispatch is much more performant than dynamic predicate dispatch
 *     since resolving a static dispatch target is {@code O(1)} and static dispatch results can be
 *     cached for the lifetime of the application with minimal memory overhead (since static
 *     dispatch policy execution doesn't depend on the input values). Whereas, dynamic dispatch
 *     policy execution is {@code O(n * m)} where {@code n} is the number of inputs whose values are
 *     being used for pattern matching and {@code m} is the number of cases in the predicate policy
 *     for a trait. Also, caching this result is not trivial since the number of possible input
 *     values is unbounded. (From <a
 *     href="https://en.wikipedia.org/wiki/Multiple_dispatch#Efficiency">wikipedia</a> : Efficient
 *     implementation of multiple-dispatch remains an ongoing research problem.)
 */
public abstract non-sealed class PredicateDispatchPolicy extends DynamicDispatchPolicy {
  public abstract ImmutableList<DispatchCase> dispatchCases();

  @Override
  public final @Nullable VajramID getDispatchTargetID(
      @Nullable Dependency dependency, Request<?> request) {
    for (DispatchCase dispatchCase : dispatchCases()) {
      var dispatchTarget = dispatchCase.computeDispatchTarget(request);
      if (dispatchTarget.isPresent()) {
        VajramID dispatchTargetID = getVajramIdByVajramReqType(dispatchTarget.get());
        validateDispatchTarget(dispatchTargetID);
        return dispatchTargetID;
      }
    }
    return null;
  }

  protected abstract VajramID getVajramIdByVajramReqType(Class<? extends Request<?>> aClass);
}
