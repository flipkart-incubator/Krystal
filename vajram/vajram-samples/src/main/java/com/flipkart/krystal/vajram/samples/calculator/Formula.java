package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.data.IfNoValue.Strategy.DEFAULT_TO_ZERO;
import static com.flipkart.krystal.data.IfNoValue.Strategy.FAIL;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.Formula_Fac.a_s;
import static com.flipkart.krystal.vajram.samples.calculator.Formula_Fac.p_s;
import static com.flipkart.krystal.vajram.samples.calculator.Formula_Fac.q_s;
import static com.flipkart.krystal.vajram.samples.calculator.Formula_Fac.quotient_s;
import static com.flipkart.krystal.vajram.samples.calculator.Formula_Fac.sum_s;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.except.StackTracelessException;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.data.IfNoValue;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.samples.calculator.add.Add;
import com.flipkart.krystal.vajram.samples.calculator.add.Add_Req;
import com.flipkart.krystal.vajram.samples.calculator.divide.Divide;
import com.flipkart.krystal.vajram.samples.calculator.divide.Divide_Req;
import com.google.common.collect.ImmutableCollection;

/** a/(p+q) */
@ExternallyInvocable
@Vajram
public abstract class Formula extends ComputeVajramDef<Integer> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Facets {

    /** The numerator */
    @Input int a;

    /** First addend of the denominator */
    @Input
    @IfNoValue(then = DEFAULT_TO_ZERO)
    int p;

    /** Second addend of the denominator */
    @IfNoValue(then = FAIL)
    @Input
    int q;

    /** The denominator */
    @IfNoValue(then = FAIL)
    @Dependency(onVajram = Add.class)
    int sum;

    /**
     * The final result to be returned. This is the result of the computation
     *
     * <pre>{@code a/(p+q)} </pre>
     */
    @Dependency(onVajram = Divide.class)
    int quotient;
  }

  @Override
  public ImmutableCollection<SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        /* sum = adder(numberOne=p, numberTwo=q) */
        dep(
            sum_s,
            depInput(Add_Req.numberOne_s).usingAsIs(p_s).asResolver(),
            depInput(Add_Req.numberTwo_s).usingAsIs(q_s).asResolver()),
        /* quotient = divider(numerator = a, denominator= sum) */
        dep(
            quotient_s,
            depInput(Divide_Req.numerator_s).usingAsIs(a_s).asResolver(),
            depInput(Divide_Req.denominator_s).usingAsIs(sum_s).asResolver()));
  }

  @Output
  static int result(Errable<Integer> quotient, int q) throws Throwable {
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
