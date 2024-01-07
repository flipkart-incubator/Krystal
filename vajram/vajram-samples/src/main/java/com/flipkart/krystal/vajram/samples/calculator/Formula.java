package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.FormulaInputUtil.*;
import static com.flipkart.krystal.vajram.samples.calculator.FormulaRequest.*;
import static com.flipkart.krystal.vajram.samples.calculator.adder.AdderRequest.*;
import static com.flipkart.krystal.vajram.samples.calculator.divider.DividerRequest.*;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Dependency;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.samples.calculator.FormulaInputUtil.FormulaInputs;
import com.flipkart.krystal.vajram.samples.calculator.adder.Adder;
import com.flipkart.krystal.vajram.samples.calculator.divider.Divider;
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
            depInput(numberOne_s).usingAsIs(p_s).asResolver(),
            depInput(numberTwo_s).usingAsIs(q_s).asResolver()),
        /* quotient = divider(numerator = a, denominator= sum) */
        dep(
            quotient_s,
            depInput(numerator_s).usingAsIs(a_s).asResolver(),
            depInput(denominator_s).usingAsIs(sum_s).asResolver()));
  }

  @VajramLogic
  static int result(FormulaInputs allInputs) {
    /* Return quotient */
    return allInputs.quotient();
  }
}
