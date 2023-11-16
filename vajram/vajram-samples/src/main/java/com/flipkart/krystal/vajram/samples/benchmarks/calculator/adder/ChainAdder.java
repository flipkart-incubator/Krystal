package com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder;

import static com.flipkart.krystal.vajram.inputs.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajram.inputs.MultiExecute.skipFanout;
import static com.flipkart.krystal.vajram.inputs.SingleExecute.executeWith;
import static com.flipkart.krystal.vajram.inputs.SingleExecute.skipExecution;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderRequest.numberOne_n;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderRequest.numberTwo_n;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.ChainAdderRequest.chainSum_n;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.ChainAdderRequest.numbers_n;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.ChainAdderRequest.sum_n;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Dependency;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.MultiExecute;
import com.flipkart.krystal.vajram.inputs.SingleExecute;
import com.flipkart.krystal.vajram.inputs.Using;
import com.flipkart.krystal.vajram.inputs.resolution.Resolve;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.ChainAdderInputUtil.ChainAdderInputs;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

@VajramDef(ChainAdder.ID)
public abstract class ChainAdder extends ComputeVajram<Integer> {

  public static final String ID = "chainAdder";

  @Input List<Integer> numbers;

  @Dependency(value = ID, canFanout = true)
  Integer chainSum;

  @Dependency(Adder.ID)
  Integer sum;

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
    if (numbers.isEmpty()) {
      return skipExecution("No numbers provided. Skipping adder call");
    } else if (numbers.size() > 2) {
      return skipExecution(
          "Cannot call adder for more than 2 inputs. ChainAdder will be called instead");
    } else {
      return executeWith(numbers.get(0));
    }
  }

  @Resolve(depName = sum_n, depInputs = numberTwo_n)
  public static SingleExecute<Integer> adderNumberTwo(@Using("numbers") List<Integer> numbers) {
    if (numbers.isEmpty()) {
      return skipExecution("No numbers provided. Skipping adder call");
    } else if (numbers.size() > 2) {
      return skipExecution(
          "Cannot call adder for more than 2 inputs. ChainAdder will be called instead");
    } else if (numbers.size() == 1) {
      return executeWith(0);
    } else {
      return executeWith(numbers.get(1));
    }
  }

  @VajramLogic
  public static Integer add(ChainAdderInputs allInputs) {
    return allInputs.sum().orElse(0)
        + allInputs.chainSum().values().stream().mapToInt(value -> value.value().orElse(0)).sum();
  }
}
