package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.A2MinusB2_Fac.a_s;
import static com.flipkart.krystal.vajram.samples.calculator.A2MinusB2_Fac.b_s;
import static com.flipkart.krystal.vajram.samples.calculator.A2MinusB2_Fac.diff_s;
import static com.flipkart.krystal.vajram.samples.calculator.A2MinusB2_Fac.twoA_s;
import static com.flipkart.krystal.vajram.samples.calculator.A2MinusB2_Fac.twoB_s;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.data.IfNoValue;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.samples.calculator.multiply.Multiply;
import com.flipkart.krystal.vajram.samples.calculator.multiply.Multiply_Req;
import com.flipkart.krystal.vajram.samples.calculator.subtract.Subtract;
import com.flipkart.krystal.vajram.samples.calculator.subtract.Subtract_Req;
import com.google.common.collect.ImmutableCollection;

/** Computes a*2 - b*2 */
@ExternallyInvocable
@Vajram
public abstract class A2MinusB2 extends ComputeVajramDef<Integer> {
  static class _Facets {
    @IfNoValue
    @Input int a;
    @IfNoValue
    @Input int b;

    /** The value of facet {@code a} multiplied by {@code 2} */
    @IfNoValue
    @Dependency(onVajram = Multiply.class)
    int twoA;

    @IfNoValue
    @Dependency(onVajram = Multiply.class)
    int twoB;

    @IfNoValue
    @Dependency(onVajram = Subtract.class)
    int diff;
  }

  @Override
  public ImmutableCollection<SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            twoA_s,
            depInput(Multiply_Req.numberOne_s).usingAsIs(a_s).asResolver(),
            depInput(Multiply_Req.numberTwo_s).usingValueAsResolver(() -> 2)),
        dep(
            twoB_s,
            depInput(Multiply_Req.numberOne_s).usingAsIs(b_s).asResolver(),
            depInput(Multiply_Req.numberTwo_s).usingValueAsResolver(() -> 2)),
        dep(
            diff_s,
            depInput(Subtract_Req.numberOne_s).usingAsIs(twoA_s).asResolver(),
            depInput(Subtract_Req.numberTwo_s).usingAsIs(twoB_s).asResolver()));
  }

  @Output
  static int result(int diff) {
    return diff;
  }
}
