package com.flipkart.krystal.traits;


import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.traits.matchers.InputValueMatcher;
import com.google.common.collect.ImmutableList;

/**
 * A {@link TraitDispatchPolicy} which allows trait invocations to be dispatched to conformant
 * Vajrams using pattern matching, allowing Krystal to support <a
 * href="https://en.wikipedia.org/wiki/Dynamic_dispatch">dynamic</a> <a
 * href="https://en.wikipedia.org/wiki/Predicate_dispatch">predicate</a> <a
 * href="https://en.wikipedia.org/wiki/Multiple_dispatch">multiple</a> dispatch
 *
 * <p>An object of this class contains patterns matchers which are mapped to inputs.The trait inputs
 * which are tagged as {@link UseForDispatch @UseForDispatch} are matched against their
 * corresponding patternto determine which vajram to dispatch the invocation to.
 *
 * <p>Since full predicate dispatch can become extremely complex to understand and maintain, the set
 * of predicates supported is {@link InputValueMatcher limite and sealed} to prevent arbitrarily
 * complex predicates.
 *
 * @implNote Use predicate dispatch only when absolutely needed, and the problem cannot be solved by
 *     static dispatch. Static dispatch is much more performant than dynamic predicate dispatch
 *     since resolving a static dipatch target is {@code O(1)} and static dispatch results can be
 *     cached for the lifetime of the application with minimal memory overhead (since static dipatch
 *     policy execution doesn't depend on the input values). Whereas, dynamic dipatch policy
 *     execution is {@code O(n * m)} where {@code n} is the number of inputs whose values are being
 *     used for pattern matching and {@code m} is the number of cases in the predicate policy for a
 *     trait. Also, caching this result is not trivial since the number of possible input values is
 *     unbounded. (From <a
 *     href="https://en.wikipedia.org/wiki/Multiple_dispatch#Efficiency">wikipedia</a> : Efficient
 *     implementation of multiple-dispatch remains an ongoing research problem.)
 */
public non-sealed interface PredicateDynamicDispatchPolicy extends TraitDispatchPolicy {
  ImmutableList<DispatchCase> dispatchCases();

  ImmutableList<Class<? extends Request<?>>> dispatchTargetReqs();
}
