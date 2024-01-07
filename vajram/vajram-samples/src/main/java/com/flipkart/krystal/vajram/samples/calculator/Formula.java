package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Dependency;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.samples.calculator.FormulaInputUtil.FormulaInputs;
import com.flipkart.krystal.vajram.samples.calculator.adder.Adder;
import com.flipkart.krystal.vajram.samples.calculator.adder.AdderRequest;
import com.flipkart.krystal.vajram.samples.calculator.divider.Divider;
import com.flipkart.krystal.vajram.samples.calculator.divider.DividerRequest;
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
            FormulaInputUtil.sum_s,
            depInput(AdderRequest.numberOne_s).usingAsIs(FormulaRequest.p_s).asResolver(),
            depInput(AdderRequest.numberTwo_s).usingAsIs(FormulaRequest.q_s).asResolver()),
        /* quotient = divider(numerator = a, denominator= sum) */
        dep(
            FormulaInputUtil.quotient_s,
            depInput(DividerRequest.numerator_s).usingAsIs(FormulaRequest.a_s).asResolver(),
            depInput(DividerRequest.denominator_s).usingAsIs(FormulaInputUtil.sum_s).asResolver()));
  }

  @VajramLogic
  static int result(FormulaInputs allInputs) {
    /* Return quotient */
    return allInputs.quotient();
  }
}
