package com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder;

import static com.flipkart.krystal.vajram.inputs.DependencyCommand.MultiExecute.skipMultiExecute;
import static com.flipkart.krystal.vajram.inputs.DependencyCommand.SingleExecute.skipSingleExecute;
import static com.flipkart.krystal.vajram.inputs.DependencyCommand.multiExecuteWith;
import static com.flipkart.krystal.vajram.inputs.DependencyCommand.singleExecuteWith;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.From;
import com.flipkart.krystal.vajram.inputs.DependencyCommand.MultiExecute;
import com.flipkart.krystal.vajram.inputs.DependencyCommand.SingleExecute;
import com.flipkart.krystal.vajram.inputs.ResolveInputsOf;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

@VajramDef(ChainAdder.ID)
public abstract class ChainAdder extends ComputeVajram<Integer> {

  public static final String ID = "chainAdder";

  @ResolveInputsOf(dep = "chain_sum", depInputs = "numbers")
  public static MultiExecute<List<Integer>> numbersForSubChainer(
      @From("numbers") List<Integer> numbers) {
    if (numbers.size() < 3) {
      return skipMultiExecute(
          "Skipping chainer as count of numbers is less than 3. Will call adder instead");
    } else {
      return multiExecuteWith(
          ImmutableList.of(
              new ArrayList<>(numbers.subList(0, numbers.size() - 1)),
              new ArrayList<>(List.of(numbers.get(numbers.size() - 1)))));
    }
  }

  @ResolveInputsOf(dep = "sum", depInputs = "number_one")
  public static SingleExecute<Integer> adderNumberOne(@From("numbers") List<Integer> numbers) {
    if (numbers.isEmpty()) {
      return skipSingleExecute("No numbers provided. Skipping adder call");
    } else if (numbers.size() > 2) {
      return skipSingleExecute(
          "Cannot call adder for more than 2 inputs. ChainAdder will be called instead");
    } else {
      return singleExecuteWith(numbers.get(0));
    }
  }

  @ResolveInputsOf(dep = "sum", depInputs = "number_two")
  public static SingleExecute<Integer> adderNumberTwo(@From("numbers") List<Integer> numbers) {
    if (numbers.isEmpty()) {
      return skipSingleExecute("No numbers provided. Skipping adder call");
    } else if (numbers.size() > 2) {
      return skipSingleExecute(
          "Cannot call adder for more than 2 inputs. ChainAdder will be called instead");
    } else if (numbers.size() == 1) {
      return singleExecuteWith(0);
    } else {
      return singleExecuteWith(numbers.get(1));
    }
  }

  @VajramLogic
  public static Integer add(ChainAdderInputUtil.ChainAdderAllInputs allInputs) {
    return allInputs.sum().values().stream().mapToInt(value -> value.value().orElse(0)).sum()
        + allInputs.chainSum().values().stream().mapToInt(value -> value.value().orElse(0)).sum();
  }
}
