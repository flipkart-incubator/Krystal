package com.flipkart.krystal.vajram.samples.calculator.adder;

import static com.flipkart.krystal.vajram.facets.FanoutCommand.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.FanoutCommand.skipFanout;
import static com.flipkart.krystal.vajram.facets.One2OneCommand.executeWith;
import static com.flipkart.krystal.vajram.facets.One2OneCommand.skipExecution;
import static com.flipkart.krystal.vajram.samples.calculator.adder.ChainAdder_Fac.chainSum_i;
import static com.flipkart.krystal.vajram.samples.calculator.adder.ChainAdder_Fac.sum_i;
import static com.flipkart.krystal.vajram.samples.calculator.adder.ChainAdder_Req.numbers_i;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.One2OneCommand;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.Using;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExternalInvocation(allow = true)
@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class ChainAdder extends ComputeVajram<Integer> {
  static class _Facets {
    @Mandatory @Input List<Integer> numbers;

    @Dependency(onVajram = ChainAdder.class, canFanout = true)
    int chainSum;

    @Dependency(onVajram = Adder.class)
    int sum;
  }

  @Resolve(dep = chainSum_i, depInputs = numbers_i)
  public static FanoutCommand<List<Integer>> numbersForSubChainer(
      @Using(numbers_i) List<Integer> numbers) {
    if (numbers.size() < 3) {
      return skipFanout(
          "Skipping chainer as count of numbers is less than 3. Will call adder instead");
    } else {
      return executeFanoutWith(
          ImmutableList.of(
              new ArrayList<>(numbers.subList(0, numbers.size() - 1)),
              new ArrayList<>(List.of(numbers.get(numbers.size() - 1)))));
    }
  }

  @Resolve(dep = sum_i, depInputs = Adder_Req.numberOne_i)
  public static One2OneCommand<Integer> adderNumberOne(@Using(numbers_i) List<Integer> numbers) {
    return skipAdder(numbers).orElseGet(() -> executeWith(numbers.get(0)));
  }

  @Resolve(dep = sum_i, depInputs = Adder_Req.numberTwo_i)
  public static One2OneCommand<Integer> adderNumberTwo(@Using(numbers_i) List<Integer> numbers) {
    return skipAdder(numbers)
        .orElseGet(
            () -> {
              if (numbers.size() == 1) {
                return executeWith(0);
              } else {
                return executeWith(numbers.get(1));
              }
            });
  }

  @Output
  static Integer add(Errable<Integer> sum, FanoutDepResponses<ChainAdder_Req, Integer> chainSum) {
    return sum.valueOpt().orElse(0)
        + chainSum.requestResponsePairs().stream()
            .mapToInt(response -> response.response().valueOpt().orElse(0))
            .sum();
  }

  private static Optional<One2OneCommand<Integer>> skipAdder(List<Integer> numbers) {
    if (numbers.isEmpty()) {
      return Optional.of(skipExecution("No numbers provided. Skipping adder call"));
    } else if (numbers.size() > 2) {
      return Optional.of(
          skipExecution(
              "Cannot call adder for more than 2 inputs. ChainAdder will be called instead"));
    } else {
      return Optional.empty();
    }
  }
}
