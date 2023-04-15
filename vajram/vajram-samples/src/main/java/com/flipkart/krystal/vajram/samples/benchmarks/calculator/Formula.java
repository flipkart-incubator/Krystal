package com.flipkart.krystal.vajram.samples.benchmarks.calculator;

import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.Formula.ID;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajram.inputs.Using;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaInputUtil.FormulaAllInputs;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderRequest;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.DividerRequest;

/** a/(p+q) */
@VajramDef(ID)
public abstract class Formula extends ComputeVajram<Integer> {
  public static final String ID = "formula";

  @Resolve(depName = "sum", depInputs = "number_one")
  public static int adderNumberOne(@Using("p") int p) {
    return p;
  }

  @Resolve(depName = "sum", depInputs = "number_two")
  public static int adderNumberTwo(@Using("q") int q) {
    return q;
  }

  @Resolve(depName = "quotient", depInputs = "number_one")
  public static int quotientNumberOne(@Using("a") int a) {
    return a;
  }

  @Resolve(depName = "quotient", depInputs = "number_two")
  public static int quotientNumberTwo(@Using("sum") int sum) {
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
