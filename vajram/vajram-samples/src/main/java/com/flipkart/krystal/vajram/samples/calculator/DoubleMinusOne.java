package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.facets.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOne_Fac.doubledNumbers_i;
import static com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOne_Fac.doubledNumbers_s;
import static com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOne_Fac.result_i;
import static com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOne_Fac.result_s;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.RequestResponse;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.MultiExecute;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.sdk.Resolve;
import com.flipkart.krystal.vajram.samples.calculator.multiplier.Multiplier;
import com.flipkart.krystal.vajram.samples.calculator.multiplier.Multiplier_Req;
import com.flipkart.krystal.vajram.samples.calculator.subtractor.Subtractor;
import com.flipkart.krystal.vajram.samples.calculator.subtractor.Subtractor_Req;
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
  public ImmutableCollection<SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(doubledNumbers_s, depInput(Multiplier_Req.numberTwo_s).usingValueAsResolver(() -> 2)),
        dep(result_s, depInput(Subtractor_Req.numberTwo_s).usingValueAsResolver(() -> 1)));
  }

  @Resolve(dep = doubledNumbers_i, depInputs = Multiplier_Req.numberOne_i)
  static MultiExecute<Integer> numbersToDouble(List<Integer> numbers) {
    return executeFanoutWith(numbers);
  }

  @Resolve(dep = result_i, depInputs = Subtractor_Req.numberOne_i)
  static int sumOfDoubles(FanoutDepResponses<Multiplier_Req, Integer> doubledNumbers) {
    return doubledNumbers.requestResponsePairs().stream()
        .map(RequestResponse::response)
        .map(Errable::valueOpt)
        .map(Optional::orElseThrow)
        .mapToInt(Integer::intValue)
        .sum();
  }

  @Output
  static int output(int result) {
    return result;
  }
}
