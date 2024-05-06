package com.flipkart.krystal.vajram.samples.calculator.adder;

import static com.flipkart.krystal.vajram.facets.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.MultiExecute.skipFanout;
import static com.flipkart.krystal.vajram.facets.SingleExecute.executeWith;
import static com.flipkart.krystal.vajram.facets.SingleExecute.skipExecution;
import static com.flipkart.krystal.vajram.samples.calculator.adder.AdderRequest.numberOne_n;
import static com.flipkart.krystal.vajram.samples.calculator.adder.AdderRequest.numberTwo_n;
import static com.flipkart.krystal.vajram.samples.calculator.adder.ChainAdderFacets.chainSum_n;
import static com.flipkart.krystal.vajram.samples.calculator.adder.ChainAdderFacets.sum_n;
import static com.flipkart.krystal.vajram.samples.calculator.adder.ChainAdderRequest.numbers_n;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.MultiExecute;
import com.flipkart.krystal.vajram.facets.SingleExecute;
import com.flipkart.krystal.vajram.facets.Using;
import com.flipkart.krystal.vajram.facets.resolution.sdk.Resolve;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class ChainAdder extends ComputeVajram<Integer> {
  static class _Facets {
    @Input List<Integer> numbers;

    @Dependency(onVajram = ChainAdder.class, canFanout = true)
    int chainSum;

    @Dependency(onVajram = Adder.class)
    Optional<Integer> sum;
  }

  @Resolve(depName = chainSum_n, depInputs = numbers_n)
  public static MultiExecute<List<Integer>> numbersForSubChainer(
      @Using(numbers_n) List<Integer> numbers) {
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

  @Resolve(depName = sum_n, depInputs = numberOne_n)
  public static SingleExecute<Integer> adderNumberOne(@Using(numbers_n) List<Integer> numbers) {
    return skipAdder(numbers).orElseGet(() -> executeWith(numbers.get(0)));
  }

  @Resolve(depName = sum_n, depInputs = numberTwo_n)
  public static SingleExecute<Integer> adderNumberTwo(@Using(numbers_n) List<Integer> numbers) {
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
  static Integer add(ChainAdderFacets facets) {
    return facets.sum().valueOpt().orElse(0)
        + facets.chainSum().requestResponses().stream()
            .mapToInt(response -> response.response().valueOpt().orElse(0))
            .sum();
  }

  private static Optional<SingleExecute<Integer>> skipAdder(List<Integer> numbers) {
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
