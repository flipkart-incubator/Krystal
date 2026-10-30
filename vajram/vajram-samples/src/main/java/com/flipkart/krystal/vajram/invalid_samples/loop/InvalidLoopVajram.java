package com.flipkart.krystal.vajram.invalid_samples.loop;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.invalid_samples.loop.InvalidLoopVajram_Fac.first_s;
import static com.flipkart.krystal.vajram.invalid_samples.loop.InvalidLoopVajram_Fac.second_s;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.samples.calculator.add.Add;
import com.flipkart.krystal.vajram.samples.calculator.add.Add_Req;
import com.google.common.collect.ImmutableCollection;

/**
 * INVALID on purpose: {@code first} is resolved from {@code second} and vice versa. The loop is
 * defined via {@link #getSimpleInputResolvers()} so only the runtime validation (at graph load) can
 * catch it. Used only by runtime validation tests - do not load this package in other graphs.
 */
@Vajram
public abstract class InvalidLoopVajram extends ComputeVajramDef<Integer> {

  interface _InternalFacets {
    @IfAbsent(FAIL)
    @Dependency(onVajram = Add.class)
    int first();

    @IfAbsent(FAIL)
    @Dependency(onVajram = Add.class)
    int second();
  }

  @Override
  public ImmutableCollection<SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(first_s, depInput(Add_Req.numberOne_s).usingAsIs(second_s).asResolver()),
        dep(second_s, depInput(Add_Req.numberOne_s).usingAsIs(first_s).asResolver()));
  }

  @Output
  static int output(int first, int second) {
    return first + second;
  }
}
