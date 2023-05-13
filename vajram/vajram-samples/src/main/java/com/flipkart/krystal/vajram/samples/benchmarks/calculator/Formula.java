package com.flipkart.krystal.vajram.samples.benchmarks.calculator;

import static com.flipkart.krystal.vajram.inputs.ForwardingResolver.resolve;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.Formula.ID;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.FormulaInputs.a;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.FormulaInputs.p;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.FormulaInputs.q;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.FormulaInputs.quotient;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.FormulaInputs.sum;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderRequest.AdderInputs.numberOne;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderRequest.AdderInputs.numberTwo;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.DividerRequest.DividerInputs.denominator;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.DividerRequest.DividerInputs.numerator;

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
        resolve(sum, numberOne).using(p).build(),
        resolve(sum, numberTwo).using(q).build(),
        resolve(quotient, numerator).using(a).build(),
        resolve(quotient, denominator).using(sum).build());
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
