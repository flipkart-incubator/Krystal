package com.flipkart.krystal.vajram.samples.benchmarks.calculator;

import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.Formula.ID;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.BindFrom;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaInputUtil.AllInputs;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderRequest;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.DividerRequest;

/** a/(p+q) */
@VajramDef(ID)
public abstract class Formula extends ComputeVajram<Integer> {
  public static final String ID = "formula";

  @Resolve(value = "sum", inputs = "number_one")
  public static int adderNumberOne(@BindFrom("p") int p) {
    return p;
  }

  @Resolve(value = "sum", inputs = "number_two")
  public static int adderNumberTwo(@BindFrom("q") int q) {
    return q;
  }

  @Resolve(value = "quotient", inputs = "number_one")
  public static int quotientNumberOne(@BindFrom("a") int a) {
    return a;
  }

  @Resolve(value = "quotient", inputs = "number_two")
  public static int quotientNumberTwo(@BindFrom("sum") int sum) {
    return sum;
  }

  @VajramLogic
  public static int result(AllInputs allInputs) {
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
