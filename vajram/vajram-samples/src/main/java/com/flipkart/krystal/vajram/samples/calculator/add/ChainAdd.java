package com.flipkart.krystal.vajram.samples.calculator.add;

import static com.flipkart.krystal.data.IfNull.IfNullThen.FAIL;
import static com.flipkart.krystal.vajram.facets.FanoutCommand.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.FanoutCommand.skipFanout;
import static com.flipkart.krystal.vajram.facets.One2OneCommand.executeWith;
import static com.flipkart.krystal.vajram.facets.One2OneCommand.skipExecution;
import static com.flipkart.krystal.vajram.samples.calculator.add.ChainAdd_Fac.chainSum_n;
import static com.flipkart.krystal.vajram.samples.calculator.add.ChainAdd_Fac.sum_n;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.One2OneCommand;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExternallyInvocable
@Vajram
public abstract class ChainAdd extends ComputeVajramDef<Integer> implements MultiAdd {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfNull(FAIL)
    List<Integer> numbers;
  }

  static class _InternalFacets {
    @Dependency(onVajram = ChainAdd.class, canFanout = true)
    int chainSum;

    @Dependency(onVajram = Add.class)
    int sum;
  }

  @Resolve(dep = chainSum_n, depInputs = ChainAdd_Req.numbers_n)
  public static FanoutCommand<List<Integer>> numbersForSubChainer(List<Integer> numbers) {
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

  @Resolve(dep = sum_n, depInputs = Add_Req.numberOne_n)
  public static One2OneCommand<Integer> adderNumberOne(List<Integer> numbers) {
    return skipAdder(numbers).orElseGet(() -> executeWith(numbers.get(0)));
  }

  @Resolve(dep = sum_n, depInputs = Add_Req.numberTwo_n)
  public static One2OneCommand<Integer> adderNumberTwo(List<Integer> numbers) {
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
  static Integer add(Errable<Integer> sum, FanoutDepResponses<ChainAdd_Req, Integer> chainSum) {
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
