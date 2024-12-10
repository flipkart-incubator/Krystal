package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.A2MinusB2Facets.diff_s;
import static com.flipkart.krystal.vajram.samples.calculator.A2MinusB2Facets.twoA_s;
import static com.flipkart.krystal.vajram.samples.calculator.A2MinusB2Facets.twoB_s;
import static com.flipkart.krystal.vajram.samples.calculator.A2MinusB2Request.a_s;
import static com.flipkart.krystal.vajram.samples.calculator.A2MinusB2Request.b_s;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.samples.calculator.multiplier.Multiplier;
import com.flipkart.krystal.vajram.samples.calculator.multiplier.MultiplierRequest;
import com.flipkart.krystal.vajram.samples.calculator.subtractor.Subtractor;
import com.flipkart.krystal.vajram.samples.calculator.subtractor.SubtractorRequest;
import com.google.common.collect.ImmutableCollection;

/** Computes a*2 - b*2 */
@ExternalInvocation(allow = true)
@VajramDef
public abstract class A2MinusB2 extends ComputeVajram<Integer> {
  static class _Facets {
    @Input int a;
    @Input int b;

    @Dependency(onVajram = Multiplier.class)
    int twoA;

    @Dependency(onVajram = Multiplier.class)
    int twoB;

    @Dependency(onVajram = Subtractor.class)
    int diff;
  }

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            twoA_s,
            depInput(MultiplierRequest.numberOne_s).usingAsIs(a_s).asResolver(),
            depInput(MultiplierRequest.numberTwo_s).usingValueAsResolver(() -> 2)),
        dep(
            twoB_s,
            depInput(MultiplierRequest.numberOne_s).usingAsIs(b_s).asResolver(),
            depInput(MultiplierRequest.numberTwo_s).usingValueAsResolver(() -> 2)),
        dep(
            diff_s,
            depInput(SubtractorRequest.numberOne_s).usingAsIs(twoA_s).asResolver(),
            depInput(SubtractorRequest.numberTwo_s).usingAsIs(twoB_s).asResolver()));
  }

  @Output
  static int result(A2MinusB2Facets _allFacets) {
    return _allFacets.diff();
  }
}
