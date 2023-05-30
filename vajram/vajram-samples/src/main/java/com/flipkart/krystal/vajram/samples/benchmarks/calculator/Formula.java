package com.flipkart.krystal.vajram.samples.benchmarks.calculator;

import static com.flipkart.krystal.vajram.inputs.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.Formula.ID;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.a_s;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.p_s;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.q_s;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.quotient_s;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.sum_s;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.resolution.InputResolver;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaInputUtil.FormulaAllInputs;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderRequest;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.DividerRequest;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;

/** a/(p+q) */
@VajramDef(ID)
public abstract class Formula extends ComputeVajram<Integer> {
  static final String ID = "formula";

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return ImmutableList.of(
        /* sum = p+q */
        /* sum = adder(numberOne=p, numberTwo=q) */
        resolve(sum_s, AdderRequest.numberOne_s).usingAsIs(p_s).asResolver(),
        resolve(sum_s, AdderRequest.numberTwo_s).usingAsIs(q_s).asResolver(),

        /* quotient = a / sum */
        /* quotient = divider(numerator = a, denominator= sum) */
        resolve(quotient_s, DividerRequest.numerator_s).usingAsIs(a_s).asResolver(),
        resolve(quotient_s, DividerRequest.denominator_s).usingAsIs(sum_s).asResolver());
  }

  @VajramLogic
  public static int result(FormulaAllInputs allInputs) {
    /* Return quotient */
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
