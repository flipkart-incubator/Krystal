package com.flipkart.krystal.vajram.samples.calculator.adder;

import static com.flipkart.krystal.vajram.facets.SingleExecute.executeWith;
import static com.flipkart.krystal.vajram.facets.SingleExecute.skipExecution;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Dependency;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.facets.SingleExecute;
import com.flipkart.krystal.vajram.facets.Using;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.samples.calculator.adder.SplitAdderInputUtil.SplitAdderInputs;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class SplitAdder extends ComputeVajram<Integer> {

  @Input List<Integer> numbers;

  @Dependency(onVajram = SplitAdder.class)
  Optional<Integer> splitSum1;

  @Dependency(onVajram = SplitAdder.class)
  Optional<Integer> splitSum2;

  @Dependency(onVajram = Adder.class)
  Optional<Integer> sum;

  @Resolve(depName = SplitAdderRequest.splitSum1_n, depInputs = SplitAdderRequest.numbers_n)
  public static SingleExecute<ArrayList<Integer>> numbersForSubSplitter1(
      @Using(SplitAdderRequest.numbers_n) List<Integer> numbers) {
    if (numbers.size() < 2) {
      return skipExecution(
          "Skipping splitters as count of numbers is less than 2. Will call adder instead");
    } else {
      int subListSize = numbers.size() / 2;
      return executeWith(new ArrayList<>(numbers.subList(0, subListSize)));
    }
  }

  @Resolve(depName = SplitAdderRequest.splitSum2_n, depInputs = SplitAdderRequest.numbers_n)
  public static SingleExecute<ArrayList<Integer>> numbersForSubSplitter2(
      @Using(SplitAdderRequest.numbers_n) List<Integer> numbers) {
    if (numbers.size() < 2) {
      return skipExecution(
          "Skipping splitters as count of numbers is less than 2. Will call adder instead");
    } else {
      int subListSize = numbers.size() / 2;
      return executeWith(new ArrayList<>(numbers.subList(subListSize, numbers.size())));
    }
  }

  @Resolve(depName = SplitAdderRequest.sum_n, depInputs = AdderRequest.numberOne_n)
  public static SingleExecute<Integer> adderNumberOne(
      @Using(SplitAdderRequest.numbers_n) List<Integer> numbers) {
    if (numbers.size() == 1) {
      return executeWith(numbers.get(0));
    } else if (numbers.isEmpty()) {
      return skipExecution("No numbers provided. Skipping adder call");
    } else {
      return skipExecution("More than 1 numbers provided. SplitAdders will be called instead");
    }
  }

  @Resolve(depName = SplitAdderRequest.sum_n, depInputs = AdderRequest.numberTwo_n)
  public static Integer adderNumberTwo(@Using(SplitAdderRequest.numbers_n) List<Integer> numbers) {
    return 0;
  }

  @VajramLogic
  static Integer add(SplitAdderInputs allInputs) {
    return allInputs.splitSum1().orElse(0)
        + allInputs.splitSum2().orElse(0)
        + allInputs.sum().orElse(0);
  }
}
