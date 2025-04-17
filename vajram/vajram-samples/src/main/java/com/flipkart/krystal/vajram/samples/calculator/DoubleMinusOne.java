package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.data.IfNull.IfNullThen.FAIL;
import static com.flipkart.krystal.vajram.facets.FanoutCommand.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInputFanout;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOne_Fac.doubledNumbers_n;
import static com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOne_Fac.doubledNumbers_s;
import static com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOne_Fac.numbers_s;
import static com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOne_Fac.result_n;
import static com.flipkart.krystal.vajram.samples.calculator.DoubleMinusOne_Fac.result_s;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.data.RequestResponse;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.Input;
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
@Vajram
public abstract class DoubleMinusOne extends ComputeVajramDef<Integer> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfNull(FAIL)
    List<Integer> numbers;
  }

  static class _InternalFacets {
    @IfNull(FAIL)
    @Dependency(onVajram = Multiply.class, canFanout = true)
    int doubledNumbers;

    @IfNull(FAIL)
    @Dependency(onVajram = Subtract.class)
    int result;
  }

  @Override
  public ImmutableCollection<SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            doubledNumbers_s,
            depInputFanout(Multiply_Req.numberOne_s)
                .using(numbers_s)
                .asResolver(numbers -> numbers),
            depInput(Multiply_Req.numberTwo_s).usingValueAsResolver(() -> 2)),
        dep(
            result_s,
            depInput(Subtract_Req.numberOne_s)
                .using(doubledNumbers_s)
                .asResolver(
                    doubledNumbers ->
                        doubledNumbers.requestResponsePairs().stream()
                            .map(RequestResponse::response)
                            .map(Errable::valueOpt)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .mapToInt(Integer::intValue)
                            .sum()),
            depInput(Subtract_Req.numberTwo_s).usingValueAsResolver(() -> 1)));
  }

  @Output
  static int output(int result) {
    return result;
  }
}
