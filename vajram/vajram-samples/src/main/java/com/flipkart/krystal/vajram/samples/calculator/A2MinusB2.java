package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.A2MinusB2_Fac.a_s;
import static com.flipkart.krystal.vajram.samples.calculator.A2MinusB2_Fac.b_s;
import static com.flipkart.krystal.vajram.samples.calculator.A2MinusB2_Fac.diff_s;
import static com.flipkart.krystal.vajram.samples.calculator.A2MinusB2_Fac.twoA_s;
import static com.flipkart.krystal.vajram.samples.calculator.A2MinusB2_Fac.twoB_s;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.samples.calculator.multiplier.Multiplier;
import com.flipkart.krystal.vajram.samples.calculator.multiplier.Multiplier_Req;
import com.flipkart.krystal.vajram.samples.calculator.subtractor.Subtractor;
import com.flipkart.krystal.vajram.samples.calculator.subtractor.Subtractor_Req;
import com.google.common.collect.ImmutableCollection;

/** Computes a*2 - b*2 */
@ExternalInvocation(allow = true)
@VajramDef
public abstract class A2MinusB2 extends ComputeVajram<Integer> {
  static class _Facets {
    @Mandatory @Input int a;
    @Mandatory @Input int b;

    /** The value of facet {@code a} multiplied by {@code 2} */
    @Mandatory
    @Dependency(onVajram = Multiplier.class)
    int twoA;

    @Mandatory
    @Dependency(onVajram = Multiplier.class)
    int twoB;

    @Mandatory
    @Dependency(onVajram = Subtractor.class)
    int diff;
  }

  @Override
  public ImmutableCollection<SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            twoA_s,
            depInput(Multiplier_Req.numberOne_s).usingAsIs(a_s).asResolver(),
            depInput(Multiplier_Req.numberTwo_s).usingValueAsResolver(() -> 2)),
        dep(
            twoB_s,
            depInput(Multiplier_Req.numberOne_s).usingAsIs(b_s).asResolver(),
            depInput(Multiplier_Req.numberTwo_s).usingValueAsResolver(() -> 2)),
        dep(
            diff_s,
            depInput(Subtractor_Req.numberOne_s).usingAsIs(twoA_s).asResolver(),
            depInput(Subtractor_Req.numberTwo_s).usingAsIs(twoB_s).asResolver()));
  }

  @Output
  static int result(int diff) {
    return diff;
  }
}
