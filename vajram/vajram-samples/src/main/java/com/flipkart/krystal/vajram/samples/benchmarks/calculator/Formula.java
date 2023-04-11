package com.flipkart.krystal.vajram.samples.benchmarks.calculator;

import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.Formula.ID;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.From;
import com.flipkart.krystal.vajram.inputs.ResolveDep;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaInputUtil.FormulaAllInputs;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderRequest;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.DividerRequest;

/** a/(p+q) */
@VajramDef(ID)
public abstract class Formula extends ComputeVajram<Integer> {
  public static final String ID = "formula";

  @ResolveDep(depName = "sum", depInputs = "number_one")
  public static int adderNumberOne(@From("p") int p) {
    return p;
  }

  @ResolveDep(depName = "sum", depInputs = "number_two")
  public static int adderNumberTwo(@From("q") int q) {
    return q;
  }

  @ResolveDep(depName = "quotient", depInputs = "number_one")
  public static int quotientNumberOne(@From("a") int a) {
    return a;
  }

  @ResolveDep(depName = "quotient", depInputs = "number_two")
  public static int quotientNumberTwo(@From("sum") int sum) {
    return sum;
  }

  @VajramLogic
  public static int result(FormulaAllInputs allInputs) {
    return allInputs
        .quotient()
        .get(
            DividerRequest.builder()
                .numberOne(allInputs.a())
                .numberTwo(
                    allInputs
                        .sum()
                        .get(
                            AdderRequest.builder()
                                .numberOne(allInputs.p())
                                .numberTwo(allInputs.q())
                                .build())
                        .value()
                        .orElseThrow())
                .build())
        .value()
        .orElseThrow();
  }
}
