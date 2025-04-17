package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.data.IfNull.IfNullThen.FAIL;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.Add2And3_Fac.sumOf2And3_s;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.samples.calculator.add.Add;
import com.flipkart.krystal.vajram.samples.calculator.add.Add_Req;
import com.google.common.collect.ImmutableCollection;

/** Returns the sum of numbers '2' and '3' */
@ExternallyInvocable
@Vajram
abstract class Add2And3 extends ComputeVajramDef<Integer> {

  static class _InternalFacets {
    @IfNull(FAIL)
    @Dependency(onVajram = Add.class)
    int sumOf2And3;
  }

  @Override
  public ImmutableCollection<SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            sumOf2And3_s,
            depInput(Add_Req.numberOne_s).usingValueAsResolver(() -> 2),
            depInput(Add_Req.numberTwo_s).usingValueAsResolver(() -> 3)));
  }

  @Output
  static int sum(int sumOf2And3) {
    return sumOf2And3;
  }
}
