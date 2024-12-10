package com.flipkart.krystal.vajram.samples.calculator.addzero;

import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.addzero.AddZeroFacets.sum_s;
import static com.flipkart.krystal.vajram.samples.calculator.addzero.AddZeroRequest.number_s;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.samples.calculator.adder.Adder;
import com.flipkart.krystal.vajram.samples.calculator.adder.AdderRequest;
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
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return resolve(dep(sum_s, depInput(AdderRequest.numberOne_s).usingAsIs(number_s).asResolver()));
  }

  @Output
  static int output(int sum) {
    return sum;
  }
}
