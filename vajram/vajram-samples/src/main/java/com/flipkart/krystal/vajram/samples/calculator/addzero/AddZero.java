package com.flipkart.krystal.vajram.samples.calculator.addzero;

import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.addzero.AddZero_Fac.number_s;
import static com.flipkart.krystal.vajram.samples.calculator.addzero.AddZero_Fac.sum_s;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.samples.calculator.adder.Adder;
import com.flipkart.krystal.vajram.samples.calculator.adder.Adder_Req;
import com.google.common.collect.ImmutableCollection;

@VajramDef
@ExternalInvocation(allow = true)
public abstract class AddZero extends ComputeVajram<Integer> {
  static class _Facets {
    @Input int number;

    @Dependency(onVajram = Adder.class)
    int sum;
  }

  @Override
  public ImmutableCollection<SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(dep(sum_s, depInput(Adder_Req.numberOne_s).usingAsIs(number_s).asResolver()));
  }

  @Output
  static int output(int sum) {
    return sum;
  }
}
