package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.facets.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOneFacetUtil.doubledNumbers_s;
import static com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOneFacetUtil.result_s;
import static com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOneRequest.doubledNumbers_n;
import static com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOneRequest.result_n;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Dependency;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.MultiExecute;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.facets.resolution.sdk.Resolve;
import com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOneFacetUtil.DoubleMinusOneFacets;
import com.flipkart.krystal.vajram.samples.calculator.multiplier.Multiplier;
import com.flipkart.krystal.vajram.samples.calculator.multiplier.MultiplierRequest;
import com.flipkart.krystal.vajram.samples.calculator.subtractor.Subtractor;
import com.flipkart.krystal.vajram.samples.calculator.subtractor.SubtractorRequest;
import com.google.common.collect.ImmutableCollection;
import java.util.List;
import java.util.Optional;

/**
 * Takes a list of numbers, doubles each of them, adds them up and then subtracts 1 from the sum.
 */
@ExternalInvocation(allow = true)
@VajramDef
public abstract class DoubleMinusOne extends ComputeVajram<Integer> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Facets {
    @Input List<Integer> numbers;

    @Dependency(onVajram = Multiplier.class, canFanout = true)
    int doubledNumbers;

    @Dependency(onVajram = Subtractor.class)
    int result;
  }

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            doubledNumbers_s,
            depInput(MultiplierRequest.numberTwo_s).usingValueAsResolver(() -> 2)),
        dep(result_s, depInput(SubtractorRequest.numberTwo_s).usingValueAsResolver(() -> 1)));
  }

  @Resolve(depName = doubledNumbers_n, depInputs = MultiplierRequest.numberOne_n)
  static MultiExecute<Integer> numbersToDouble(List<Integer> numbers) {
    return executeFanoutWith(numbers);
  }

  @Resolve(depName = result_n, depInputs = SubtractorRequest.numberOne_n)
  static int sumOfDoubles(DependencyResponse<MultiplierRequest, Integer> doubledNumbers) {
    return doubledNumbers.values().stream()
        .map(Errable::value)
        .map(Optional::orElseThrow)
        .mapToInt(Integer::intValue)
        .sum();
  }

  @Output
  static int output(DoubleMinusOneFacets facets) {
    return facets.result();
  }
}
