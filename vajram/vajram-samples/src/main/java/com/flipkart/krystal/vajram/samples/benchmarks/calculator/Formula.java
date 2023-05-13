package com.flipkart.krystal.vajram.samples.benchmarks.calculator;

import static com.flipkart.krystal.vajram.inputs.ForwardingResolver.resolve;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.Formula.ID;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.a_s;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.p_s;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.q_s;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.quotient_s;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.sum_s;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.InputResolver;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaInputUtil.FormulaAllInputs;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderRequest;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.DividerRequest;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;

/** a/(p+q) */
@VajramDef(ID)
public abstract class Formula extends ComputeVajram<Integer> {
  public static final String ID = "formula";

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return ImmutableList.of(
        resolve(sum_s, AdderRequest.numberOne_s).using(p_s).build(),
        resolve(sum_s, AdderRequest.numberTwo_s).using(q_s).build(),
        resolve(quotient_s, DividerRequest.numerator_s).using(a_s).build(),
        resolve(quotient_s, DividerRequest.denominator_s).using(sum_s).build());
  }

  @VajramLogic
  public static int result(FormulaAllInputs allInputs) {
    return allInputs
        .quotient()
        .get(
            DividerRequest.builder()
                .numerator(allInputs.a())
                .denominator(
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
