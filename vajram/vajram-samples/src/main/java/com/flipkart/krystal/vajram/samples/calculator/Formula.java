package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.Formula_Fac.a_s;
import static com.flipkart.krystal.vajram.samples.calculator.Formula_Fac.p_s;
import static com.flipkart.krystal.vajram.samples.calculator.Formula_Fac.q_s;
import static com.flipkart.krystal.vajram.samples.calculator.Formula_Fac.quotient_s;
import static com.flipkart.krystal.vajram.samples.calculator.Formula_Fac.sum_s;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.except.KrystalException;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.samples.calculator.add.Add;
import com.flipkart.krystal.vajram.samples.calculator.add.Add_Req;
import com.flipkart.krystal.vajram.samples.calculator.divide.Divide;
import com.flipkart.krystal.vajram.samples.calculator.divide.Divide_Req;
import com.google.common.collect.ImmutableCollection;

/** a/(p+q) */
@InvocableOutsideGraph
@Vajram
public abstract class Formula extends ComputeVajramDef<Integer> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    /** The numerator */
    int a;

    /** First addend of the denominator */
    @IfAbsent(ASSUME_DEFAULT_VALUE)
    int p;

    /** Second addend of the denominator */
    @IfAbsent(FAIL)
    int q;
  }

  static class _InternalFacets {
    /** The denominator */
    @IfAbsent(FAIL)
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
            depInput(Divide_Req.numerator_s).using(a_s).asResolver(a -> a.valueOpt().orElse(null)),
            depInput(Divide_Req.denominator_s).usingAsIs(sum_s).asResolver()));
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
                        () -> new KrystalException("Did not receive division result")));
  }
}
