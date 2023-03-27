package com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder;

import static com.flipkart.krystal.vajram.inputs.DependencyCommand.executeWith;
import static com.flipkart.krystal.vajram.inputs.DependencyCommand.skip;
import static java.util.function.Function.identity;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.BindFrom;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.Resolve;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@VajramDef(SplitAdder.ID)
public abstract class SplitAdder extends ComputeVajram<Integer> {

  public static final String ID = "splitAdder";

  @Resolve(value = "split_sum_1", inputs = "numbers")
  public static DependencyCommand<ArrayList<Integer>> numbersForSubSplitter1(
      @BindFrom("numbers") List<Integer> numbers) {
    if (numbers.size() < 2) {
      return skip("Skipping splitters as count of numbers is less than 2. Will call adder instead");
    } else {
      int subListSize = numbers.size() / 2;
      return executeWith(new ArrayList<>(numbers.subList(0, subListSize)));
    }
  }

  @Resolve(value = "split_sum_2", inputs = "numbers")
  public static DependencyCommand<ArrayList<Integer>> numbersForSubSplitter2(
      @BindFrom("numbers") List<Integer> numbers) {
    if (numbers.size() < 2) {
      return skip("Skipping splitters as count of numbers is less than 2. Will call adder instead");
    } else {
      int subListSize = numbers.size() / 2;
      return executeWith(new ArrayList<>(numbers.subList(subListSize, numbers.size())));
    }
  }

  @Resolve(value = "sum", inputs = "number_one")
  public static DependencyCommand<Integer> adderNumberOne(
      @BindFrom("numbers") List<Integer> numbers) {
    if (numbers.size() == 1) {
      return executeWith(numbers.get(0));
    } else if (numbers.isEmpty()) {
      return skip("No numbers provided. Skipping adder call");
    } else {
      return skip("More than 1 numbers provided. SplitAdders will be called instead");
    }
  }

  @Resolve(value = "sum", inputs = "number_two")
  public static Integer adderNumberTwo(@BindFrom("numbers") List<Integer> numbers) {
    return 0;
  }

  @VajramLogic
  public static Integer add(SplitAdderInputUtil.SplitAdderAllInputs allInputs) {
    return Stream.of(
            allInputs.splitSum1().values().stream().map(voe -> voe.value().orElseThrow()),
            allInputs.splitSum2().values().stream().map(voe -> voe.value().orElseThrow()),
            allInputs.sum().values().stream().map(voe -> voe.value().orElseThrow()))
        .flatMap(identity())
        .mapToInt(value -> value)
        .sum();
  }
}
