package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.Formula_Fac.a_s;
import static com.flipkart.krystal.vajram.samples.calculator.Formula_Fac.p_s;
import static com.flipkart.krystal.vajram.samples.calculator.Formula_Fac.q_s;
import static com.flipkart.krystal.vajram.samples.calculator.Formula_Fac.quotient_s;
import static com.flipkart.krystal.vajram.samples.calculator.Formula_Fac.sum_s;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.except.StackTracelessException;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.samples.calculator.adder.Adder;
import com.flipkart.krystal.vajram.samples.calculator.adder.Adder_Req;
import com.flipkart.krystal.vajram.samples.calculator.divider.Divider;
import com.flipkart.krystal.vajram.samples.calculator.divider.Divider_Req;
import com.google.common.collect.ImmutableCollection;

/** a/(p+q) */
@ExternalInvocation(allow = true)
@VajramDef
public abstract class Formula extends ComputeVajram<Integer> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Facets {
    @Input int a;
    @Input int p;
    @Input int q;

    @Mandatory
    @Dependency(onVajram = Adder.class)
    int sum;

    @Dependency(onVajram = Divider.class)
    int quotient;
  }

  @Override
  public ImmutableCollection<SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        /* sum = adder(numberOne=p, numberTwo=q) */
        dep(
            sum_s,
            depInput(Adder_Req.numberOne_s).usingAsIs(p_s).asResolver(),
            depInput(Adder_Req.numberTwo_s).usingAsIs(q_s).asResolver()),
        /* quotient = divider(numerator = a, denominator= sum) */
        dep(
            quotient_s,
            depInput(Divider_Req.numerator_s).usingAsIs(a_s).asResolver(),
            depInput(Divider_Req.denominator_s).usingAsIs(sum_s).asResolver()));
  }

  @Output
  static int result(Errable<Integer> quotient) throws Throwable {
    /* Return quotient */
    return quotient
        .valueOpt()
        .orElseThrow(
            () ->
                quotient
                    .errorOpt()
                    .orElseGet(
                        () -> new StackTracelessException("Did not receive division result")));
  }
}
