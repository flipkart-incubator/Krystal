package com.flipkart.krystal.vajram.samples.calculator.adder;

import static com.flipkart.krystal.vajram.facets.SingleExecute.executeWith;
import static com.flipkart.krystal.vajram.facets.SingleExecute.skipExecution;
import static com.flipkart.krystal.vajram.samples.calculator.adder.AdderRequest.*;
import static com.flipkart.krystal.vajram.samples.calculator.adder.SplitAdderRequest.*;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Dependency;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.SingleExecute;
import com.flipkart.krystal.vajram.facets.Using;
import com.flipkart.krystal.vajram.facets.resolution.sdk.Resolve;
import com.flipkart.krystal.vajram.samples.calculator.adder.SplitAdderFacetUtil.SplitAdderFacets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExternalInvocation(allow = true)
@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class SplitAdder extends ComputeVajram<Integer> {
  static class _Facets {
    @Input List<Integer> numbers;

    @Dependency(onVajram = SplitAdder.class)
    Optional<Integer> splitSum1;

    @Dependency(onVajram = SplitAdder.class)
    Optional<Integer> splitSum2;

    @Dependency(onVajram = Adder.class)
    Optional<Integer> sum;
  }

  @Resolve(depName = splitSum1_n, depInputs = numbers_n)
  public static SingleExecute<List<Integer>> numbersForSubSplitter1(
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
  public static SingleExecute<List<Integer>> numbersForSubSplitter2(
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

  @Output
  static Integer add(SplitAdderFacets facets) {
    return facets.splitSum1().value().orElse(0)
        + facets.splitSum2().value().orElse(0)
        + facets.sum().value().orElse(0);
  }
}
