package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.Add2And3Facets.sumOf2And3_s;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.samples.calculator.adder.Adder;
import com.flipkart.krystal.vajram.samples.calculator.adder.AdderRequest;
import com.google.common.collect.ImmutableCollection;

/** Returns the sum of numbers '2' and '3' */
@ExternalInvocation(allow = true)
@VajramDef
abstract class Add2And3 extends ComputeVajram<Integer> {
  static class _Facets {
    @Dependency(onVajram = Adder.class)
    int sumOf2And3;
  }

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            sumOf2And3_s,
            depInput(AdderRequest.numberOne_s).usingValueAsResolver(() -> 2),
            depInput(AdderRequest.numberTwo_s).usingValueAsResolver(() -> 3)));
  }

  @Output
  static int sum(Add2And3Facets _allFacets) {
    return _allFacets.sumOf2And3();
  }
}
