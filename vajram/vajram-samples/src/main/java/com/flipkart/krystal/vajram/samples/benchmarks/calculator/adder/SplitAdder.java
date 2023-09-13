package com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder;

import static com.flipkart.krystal.vajram.inputs.SingleExecute.executeWith;
import static com.flipkart.krystal.vajram.inputs.SingleExecute.skipExecution;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderRequest.numberOne_n;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderRequest.numberTwo_n;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.SplitAdderRequest.numbers_n;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.SplitAdderRequest.splitSum1_n;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.SplitAdderRequest.splitSum2_n;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.SplitAdderRequest.sum_n;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.SingleExecute;
import com.flipkart.krystal.vajram.inputs.Using;
import com.flipkart.krystal.vajram.inputs.resolution.Resolve;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.SplitAdderInputUtil.SplitAdderInputs;
import java.util.ArrayList;
import java.util.List;

@VajramDef(SplitAdder.ID)
public abstract class SplitAdder extends ComputeVajram<Integer> {

  public static final String ID = "SplitAdder";

  @Resolve(depName = splitSum1_n, depInputs = numbers_n)
  public static SingleExecute<ArrayList<Integer>> numbersForSubSplitter1(
      @Using(numbers_n) List<Integer> numbers) {
    if (numbers.size() < 2) {
      return skipExecution(
          "Skipping splitters as count of numbers is less than 2. Will call adder instead");
    } else {
      int subListSize = numbers.size() / 2;
      return executeWith(new ArrayList<>(numbers.subList(0, subListSize)));
    }
  }

  @Resolve(depName = splitSum2_n, depInputs = numbers_n)
  public static SingleExecute<ArrayList<Integer>> numbersForSubSplitter2(
      @Using(numbers_n) List<Integer> numbers) {
    if (numbers.size() < 2) {
      return skipExecution(
          "Skipping splitters as count of numbers is less than 2. Will call adder instead");
    } else {
      int subListSize = numbers.size() / 2;
      return executeWith(new ArrayList<>(numbers.subList(subListSize, numbers.size())));
    }
  }

  @Resolve(depName = sum_n, depInputs = numberOne_n)
  public static SingleExecute<Integer> adderNumberOne(@Using(numbers_n) List<Integer> numbers) {
    if (numbers.size() == 1) {
      return executeWith(numbers.get(0));
    } else if (numbers.isEmpty()) {
      return skipExecution("No numbers provided. Skipping adder call");
    } else {
      return skipExecution("More than 1 numbers provided. SplitAdders will be called instead");
    }
  }

  @Resolve(depName = sum_n, depInputs = numberTwo_n)
  public static Integer adderNumberTwo(@Using(numbers_n) List<Integer> numbers) {
    return 0;
  }

  @VajramLogic
  public static Integer add(SplitAdderInputs allInputs) {
    return allInputs.splitSum1().orElse(0)
        + allInputs.splitSum2().orElse(0)
        + allInputs.sum().orElse(0);
  }
}
