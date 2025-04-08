package com.flipkart.krystal.vajram.samples.calculator.addzero;

import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.addzero.AddZero_Fac.number_s;
import static com.flipkart.krystal.vajram.samples.calculator.addzero.AddZero_Fac.sum_s;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.data.IfNoValue;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.samples.calculator.add.Add;
import com.flipkart.krystal.vajram.samples.calculator.add.Add_Req;
import com.google.common.collect.ImmutableCollection;

@Vajram
@ExternallyInvocable
public abstract class AddZero extends ComputeVajramDef<Integer> {
  static class _Facets {
    @IfNoValue
    @Input int number;

    @IfNoValue
    @Dependency(onVajram = Add.class)
    int sum;
  }

  @Override
  public ImmutableCollection<SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(dep(sum_s, depInput(Add_Req.numberOne_s).usingAsIs(number_s).asResolver()));
  }

  @Output
  static int output(int sum) {
    return sum;
  }
}
