package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.facets.FanoutCommand.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOne_Fac.doubledNumbers_n;
import static com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOne_Fac.doubledNumbers_s;
import static com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOne_Fac.result_n;
import static com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOne_Fac.result_s;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.RequestResponse;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.samples.calculator.multiply.Multiply;
import com.flipkart.krystal.vajram.samples.calculator.multiply.Multiply_Req;
import com.flipkart.krystal.vajram.samples.calculator.subtract.Subtract;
import com.flipkart.krystal.vajram.samples.calculator.subtract.Subtract_Req;
import com.google.common.collect.ImmutableCollection;
import java.util.List;
import java.util.Optional;

/**
 * Takes a list of numbers, doubles each of them, adds them up and then subtracts 1 from the sum.
 */
@ExternalInvocation(allow = true)
@Vajram
public abstract class DoubleMinusOne extends ComputeVajramDef<Integer> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Facets {
    @Mandatory @Input List<Integer> numbers;

    @Mandatory
    @Dependency(onVajram = Multiply.class, canFanout = true)
    int doubledNumbers;

    @Mandatory
    @Dependency(onVajram = Subtract.class)
    int result;
  }

  @Override
  public ImmutableCollection<SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(doubledNumbers_s, depInput(Multiply_Req.numberTwo_s).usingValueAsResolver(() -> 2)),
        dep(result_s, depInput(Subtract_Req.numberTwo_s).usingValueAsResolver(() -> 1)));
  }

  @Resolve(dep = doubledNumbers_n, depInputs = Multiply_Req.numberOne_n)
  static FanoutCommand<Integer> numbersToDouble(List<Integer> numbers) {
    return executeFanoutWith(numbers);
  }

  @Resolve(dep = result_n, depInputs = Subtract_Req.numberOne_n)
  @SuppressWarnings("methodref.receiver")
  static int sumOfDoubles(FanoutDepResponses<Multiply_Req, Integer> doubledNumbers) {
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
