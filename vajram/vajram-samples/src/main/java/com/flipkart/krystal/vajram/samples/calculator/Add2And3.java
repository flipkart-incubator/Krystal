package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.Add2And3_Fac.sumOf2And3_s;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.samples.calculator.adder.Adder;
import com.flipkart.krystal.vajram.samples.calculator.adder.Adder_Req;
import com.google.common.collect.ImmutableCollection;

/** Returns the sum of numbers '2' and '3' */
@ExternalInvocation(allow = true)
@VajramDef
abstract class Add2And3 extends ComputeVajram<Integer> {
  static class _Facets {
    @Mandatory
    @Dependency(onVajram = Adder.class)
    int sumOf2And3;
  }

  @Override
  public ImmutableCollection<SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            sumOf2And3_s,
            depInput(Adder_Req.numberOne_s).usingValueAsResolver(() -> 2),
            depInput(Adder_Req.numberTwo_s).usingValueAsResolver(() -> 3)));
  }

  @Output
  static int sum(int sumOf2And3) {
    return sumOf2And3;
  }
}
