package com.flipkart.krystal.vajramexecutor.krystex.traits;

import static com.flipkart.krystal.traits.matchers.InputValueMatcher.isAnyValue;
import static com.google.common.base.Preconditions.checkArgument;
import static lombok.AccessLevel.PRIVATE;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.InputMirror;
import com.flipkart.krystal.traits.DispatchCase;
import com.flipkart.krystal.traits.PredicateDispatchPolicy;
import com.flipkart.krystal.traits.UseForPredicateDispatch;
import com.flipkart.krystal.traits.matchers.InputValueMatcher;
import com.flipkart.krystal.vajram.TraitRequestRoot;
import com.flipkart.krystal.vajram.VajramRequestRoot;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Value;

public class PredicateDispatchUtil {

  public static <R extends Request<?>> InputDispatcherBuilder<R> dispatchTrait(
      Class<R> traitReq, VajramKryonGraph graph) {
    checkArgument(
        traitReq.getAnnotation(TraitRequestRoot.class) != null,
        "Expecting a trait request root i.e a request class with the annotation @TraitRequestRoot");
    return new InputDispatcherBuilder<>(traitReq, graph);
  }

  public static <T, R> DispatchCaseBuilder<R> when(
      InputMirrorSpec<T, ? extends Request> input, InputValueMatcher<T> inputValueMatcher) {
    checkArgument(
        input.tags().getAnnotationByType(UseForPredicateDispatch.class).isPresent(),
        "Only the trait Inputs annotated as @UseForPredicateDispatch can be used for dynamic dispatching");
    return new DispatchCaseBuilder<>(ImmutableMap.of(input, inputValueMatcher));
  }

  @AllArgsConstructor(access = PRIVATE)
  public static class InputDispatcherBuilder<R extends Request<?>> {

    private final Class<R> traitReq;
    private final VajramKryonGraph graph;

    @SafeVarargs
    public final PredicateDispatchPolicy conditionally(
        PredicatesDispatchCase<? extends Request<?>>... dispatchCases) {
      return new PredicateDispatchPolicyImpl(traitReq, ImmutableList.copyOf(dispatchCases), graph);
    }

    public final PredicateDispatchPolicy alwaysTo(Class<? extends R> dispatchTarget) {
      return new PredicateDispatchPolicyImpl(
          traitReq,
          ImmutableList.of(new PredicatesDispatchCase<>(ImmutableMap.of(), dispatchTarget)),
          graph);
    }

    public final PredicateDispatchPolicyImpl computingTargetWith(
        Function<? super R, Optional<? extends Class<? extends R>>> dispatchTargetSelector,
        ImmutableSet<? extends Class<? extends R>> dispatchTargets) {
      return new PredicateDispatchPolicyImpl(
          traitReq,
          ImmutableList.of(
              new ComputedDispatchCase<>(
                  dispatchTargetSelector, ImmutableSet.copyOf(dispatchTargets))),
          graph);
    }
  }

  @AllArgsConstructor(access = PRIVATE)
  public static class DispatchCaseBuilder<T> {

    private ImmutableMap<InputMirror, InputValueMatcher<?>> facetPredicates;

    public <P> DispatchCaseBuilder<T> and(
        InputMirrorSpec<P, ? extends Request<? extends T>> input, InputValueMatcher<P> dataType) {
      checkArgument(
          !facetPredicates.containsKey(input),
          "Facet " + input + " already has a type check in this case");
      LinkedHashMap<InputMirror, InputValueMatcher<?>> newMap =
          new LinkedHashMap<>(facetPredicates);
      newMap.put(input, dataType);
      return new DispatchCaseBuilder<>(ImmutableMap.copyOf(newMap));
    }

    public PredicatesDispatchCase<? extends Request<? extends T>> to(
        Class<? extends Request<? extends T>> dispatchTarget) {
      checkArgument(
          dispatchTarget.getAnnotation(VajramRequestRoot.class) != null,
          "Expecting a Vajram request root class, i.e. one with the @VajramRequestRoot annotation");
      return new PredicatesDispatchCase<>(facetPredicates, dispatchTarget);
    }
  }

  @Value
  @AllArgsConstructor(access = PRIVATE)
  public static class PredicatesDispatchCase<T extends Request<?>> implements DispatchCase {

    private final ImmutableMap<InputMirror, InputValueMatcher<?>> inputPredicates;
    private final Class<? extends T> dispatchTarget;

    public ImmutableSet<InputMirror> dispatchEnabledInputs() {
      return inputPredicates.keySet();
    }

    @Override
    public Optional<Class<? extends T>> computeDispatchTarget(Request<?> request) {
      boolean caseMatches = true;
      for (InputMirror dispatchEnabledInput : dispatchEnabledInputs()) {
        Object inputValue = dispatchEnabledInput.getFromRequest(request);
        if (!inputPredicates()
            .getOrDefault(dispatchEnabledInput, isAnyValue())
            .matches(inputValue)) {
          caseMatches = false;
          break;
        }
      }
      if (caseMatches) {
        return Optional.of(dispatchTarget);
      } else {
        return Optional.empty();
      }
    }

    @Override
    public ImmutableSet<Class<? extends Request<?>>> dispatchTargets() {
      return ImmutableSet.of(dispatchTarget);
    }
  }

  private record ComputedDispatchCase<R extends Request<?>>(
      Function<? super R, Optional<? extends Class<? extends R>>> dispatchTargetSelector,
      ImmutableSet<Class<? extends Request<?>>> dispatchTargets)
      implements DispatchCase {

    @SuppressWarnings("unchecked")
    @Override
    public Optional<? extends Class<? extends Request<?>>> computeDispatchTarget(
        Request<?> request) {
      var dispatchTarget = dispatchTargetSelector.apply((R) request);
      if (dispatchTarget.isPresent() && dispatchTargets.contains(dispatchTarget.get())) {
        return dispatchTarget;
      } else {
        return Optional.empty();
      }
    }
  }

  private PredicateDispatchUtil() {}
}
