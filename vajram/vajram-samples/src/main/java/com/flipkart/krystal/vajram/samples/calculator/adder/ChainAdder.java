package com.flipkart.krystal.vajram.samples.calculator.adder;

import static com.flipkart.krystal.vajram.facets.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.MultiExecute.skipFanout;
import static com.flipkart.krystal.vajram.facets.SingleExecute.executeWith;
import static com.flipkart.krystal.vajram.facets.SingleExecute.skipExecution;
import static com.flipkart.krystal.vajram.samples.calculator.adder.AdderRequest.*;
import static com.flipkart.krystal.vajram.samples.calculator.adder.ChainAdderRequest.*;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Dependency;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.facets.MultiExecute;
import com.flipkart.krystal.vajram.facets.SingleExecute;
import com.flipkart.krystal.vajram.facets.Using;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.samples.calculator.adder.ChainAdderInputUtil.ChainAdderInputs;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class ChainAdder extends ComputeVajram<Integer> {

  @Input List<Integer> numbers;

  @Dependency(onVajram = ChainAdder.class, canFanout = true)
  int chainSum;

  @Dependency(onVajram = Adder.class)
  Optional<Integer> sum;

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

  @VajramLogic
  static Integer add(ChainAdderInputs allInputs) {
    return allInputs.sum().orElse(0)
        + allInputs.chainSum().values().stream().mapToInt(value -> value.value().orElse(0)).sum();
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
