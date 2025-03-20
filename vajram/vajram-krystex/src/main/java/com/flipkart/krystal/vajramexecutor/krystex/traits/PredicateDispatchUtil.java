package com.flipkart.krystal.vajramexecutor.krystex.traits;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static lombok.AccessLevel.PRIVATE;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.InputMirror;
import com.flipkart.krystal.traits.PredicateDynamicDispatchPolicy;
import com.flipkart.krystal.traits.PredicateDynamicDispatchPolicy.DispatchCase;
import com.flipkart.krystal.traits.UseForDispatch;
import com.flipkart.krystal.traits.matchers.InputValueMatcher;
import com.flipkart.krystal.vajram.TraitRequestRoot;
import com.flipkart.krystal.vajram.VajramRequestRoot;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import lombok.AllArgsConstructor;
import lombok.Value;

public class PredicateDispatchUtil {
  public static <T extends Request<?>> InputDispatcherBuilder<T> dispatchTrait(
      Class<T> traitReq, VajramKryonGraph graph) {
    checkArgument(
        traitReq.getAnnotation(TraitRequestRoot.class) != null,
        "Expecting a trait request root i.e a request class with the annotation @TraitRequestRoot");
    return new InputDispatcherBuilder<T>(traitReq, graph);
  }

  public static <T extends Request<?>> DispatchCaseBuilder<T> when(
      InputMirrorSpec<?, T> input, InputValueMatcher inputValueMatcher) {
    checkArgument(
        input.tags().getAnnotationByType(UseForDispatch.class).isPresent(),
        "Only the trait Inputs annotated as @UseForDispatch can be used for dynamic dispatching");
    return new DispatchCaseBuilder<>(ImmutableMap.of(input, inputValueMatcher));
  }

  @AllArgsConstructor(access = PRIVATE)
  public static class InputDispatcherBuilder<T extends Request<?>> {

    private final Class<T> traitReq;
    private final VajramKryonGraph graph;

    @SafeVarargs
    public final PredicateDynamicDispatchPolicy conditionally(
        DispatchCaseFinal<T>... dispatchCases) {
      return new PredicateDynamicDispatchPolicy(
          graph.getVajramIdByVajramReqType(traitReq),
          Arrays.stream(dispatchCases)
              .map(
                  d ->
                      new DispatchCase(
                          d.inputPredicates(),
                          graph.getVajramIdByVajramReqType(d.dispatchTarget())))
              .collect(toImmutableList()));
    }
  }

  @AllArgsConstructor(access = PRIVATE)
  public static class DispatchCaseBuilder<T extends Request<?>> {
    private ImmutableMap<InputMirror, InputValueMatcher> facetPredicates;

    public DispatchCaseBuilder<T> and(InputMirror input, InputValueMatcher dataType) {
      checkArgument(
          !facetPredicates.containsKey(input),
          "Facet " + input + " already has a type check in this case");
      LinkedHashMap<InputMirror, InputValueMatcher> newMap = new LinkedHashMap<>(facetPredicates);
      newMap.put(input, dataType);
      return new DispatchCaseBuilder<>(ImmutableMap.copyOf(newMap));
    }

    public DispatchCaseFinal<T> to(Class<? extends T> dispatchTarget) {
      checkArgument(
          dispatchTarget.getAnnotation(VajramRequestRoot.class) != null,
          "Expecting a Vajram request root class, i.e. one with the @VajramRequestRoot annotation");
      return new DispatchCaseFinal<T>(facetPredicates, dispatchTarget);
    }
  }

  @Value
  @AllArgsConstructor(access = PRIVATE)
  public static final class DispatchCaseFinal<T extends Request<?>> {
    ImmutableMap<InputMirror, InputValueMatcher> inputPredicates;
    Class<? extends T> dispatchTarget;
  }

  private PredicateDispatchUtil() {}
}
