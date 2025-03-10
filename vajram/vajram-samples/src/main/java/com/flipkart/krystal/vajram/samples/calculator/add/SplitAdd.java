package com.flipkart.krystal.vajram.samples.calculator.add;

import static com.flipkart.krystal.vajram.facets.One2OneCommand.executeWith;
import static com.flipkart.krystal.vajram.facets.One2OneCommand.skipExecution;
import static com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_Fac.splitSum1_n;
import static com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_Fac.splitSum2_n;
import static com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_Fac.sum_n;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.annos.ConformsToTrait;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.One2OneCommand;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExternalInvocation(allow = true)
@Vajram
@ConformsToTrait(withDef = MultiAdd.class)
@SuppressWarnings({"optional.parameter", "initialization.field.uninitialized"})
public abstract class SplitAdd extends ComputeVajramDef<Integer> {
  static class _Facets {
    @Mandatory @Input List<Integer> numbers;

    @Dependency(onVajram = SplitAdd.class)
    int splitSum1;

    @Dependency(onVajram = SplitAdd.class)
    int splitSum2;

    @Dependency(onVajram = Add.class)
    int sum;
  }

  @Resolve(dep = splitSum1_n, depInputs = SplitAdd_Req.numbers_n)
  public static One2OneCommand<List<Integer>> numbersForSubSplitter1(List<Integer> numbers) {
    if (numbers.size() < 2) {
      return skipExecution(
          "Skipping splitters as count of numbers is less than 2. Will call adder instead");
    } else {
      int subListSize = numbers.size() / 2;
      return executeWith(new ArrayList<>(numbers.subList(0, subListSize)));
    }
  }

  @Resolve(dep = splitSum2_n, depInputs = SplitAdd_Req.numbers_n)
  public static One2OneCommand<List<Integer>> numbersForSubSplitter2(List<Integer> numbers) {
    if (numbers.size() < 2) {
      return skipExecution(
          "Skipping splitters as count of numbers is less than 2. Will call adder instead");
    } else {
      int subListSize = numbers.size() / 2;
      return executeWith(new ArrayList<>(numbers.subList(subListSize, numbers.size())));
    }
  }

  @Resolve(dep = sum_n, depInputs = Add_Req.numberOne_n)
  public static One2OneCommand<Integer> adderNumberOne(List<Integer> numbers) {
    if (numbers.size() == 1) {
      return executeWith(numbers.get(0));
    } else if (numbers.isEmpty()) {
      return skipExecution("No numbers provided. Skipping adder call");
    } else {
      return skipExecution("More than 1 numbers provided. SplitAdders will be called instead");
    }
  }

  @Resolve(dep = sum_n, depInputs = Add_Req.numberTwo_n)
  public static Integer adderNumberTwo(List<Integer> numbers) {
    return 0;
  }

  @Output
  static Integer add(
      Optional<Integer> splitSum1, Optional<Integer> splitSum2, Optional<Integer> sum) {
    return splitSum1.orElse(0) + splitSum2.orElse(0) + sum.orElse(0);
  }
}
