package com.flipkart.krystal.vajram.samples.benchmarks.calculator;

import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.a_s;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.p_s;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.q_s;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.quotient_s;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaRequest.sum_s;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Dependency;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaInputUtil.FormulaInputs;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.Adder;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderRequest;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.Divider;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.DividerRequest;
import com.google.common.collect.ImmutableCollection;

/** a/(p+q) */
@VajramDef
public abstract class Formula extends ComputeVajram<Integer> {

  @Input int a;
  @Input int p;
  @Input int q;

  @Dependency(onVajram = Adder.class)
  int sum;

  @Dependency(onVajram = Divider.class)
  int quotient;

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return resolve(
        /* sum = adder(numberOne=p, numberTwo=q) */
        dep(
            sum_s,
            depInput(AdderRequest.numberOne_s).usingAsIs(p_s).asResolver(),
            depInput(AdderRequest.numberTwo_s).usingAsIs(q_s).asResolver()),
        /* quotient = divider(numerator = a, denominator= sum) */
        dep(
            quotient_s,
            depInput(DividerRequest.numerator_s).usingAsIs(a_s).asResolver(),
            depInput(DividerRequest.denominator_s).usingAsIs(sum_s).asResolver()));
  }

  @VajramLogic
  public static int result(FormulaInputs allInputs) {
    /* Return quotient */
    return allInputs.quotient();
  }
}
