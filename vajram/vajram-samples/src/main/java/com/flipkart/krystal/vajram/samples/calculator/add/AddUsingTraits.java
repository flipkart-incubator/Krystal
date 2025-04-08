package com.flipkart.krystal.vajram.samples.calculator.add;

import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.add.AddUsingTraits_Fac.numbers1_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.AddUsingTraits_Fac.numbers2_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.AddUsingTraits_Fac.numbers3_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.AddUsingTraits_Fac.sum1_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.AddUsingTraits_Fac.sum2_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.AddUsingTraits_Fac.sum3_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd.MultiAddType.CHAIN;
import static com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd.MultiAddType.SIMPLE;
import static com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd.MultiAddType.SPLIT;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.data.IfNoValue;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.samples.calculator.add.AddUsingTraits.ThreeSums;
import com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd.MultiAddQualifier;
import com.google.common.collect.ImmutableCollection;
import java.util.List;
import java.util.Optional;

/** Adds three lists of numbers in three different ways and then returns those sums */
@ExternallyInvocable
@Vajram
@SuppressWarnings("optional.parameter")
public abstract class AddUsingTraits extends ComputeVajramDef<ThreeSums> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Facets {
    @IfNoValue
    @Input List<Integer> numbers1;
    @IfNoValue
    @Input List<Integer> numbers2;
    @IfNoValue
    @Input List<Integer> numbers3;

    @Dependency(onVajram = MultiAdd.class)
    @MultiAddQualifier(SIMPLE)
    int sum1;

    @Dependency(onVajram = MultiAdd.class)
    @MultiAddQualifier(CHAIN)
    int sum2;

    @Dependency(onVajram = MultiAdd.class)
    @MultiAddQualifier(SPLIT)
    int sum3;
  }

  @Override
  public ImmutableCollection<? extends SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(sum1_s, depInput(MultiAdd_Req.numbers_s).usingAsIs(numbers1_s).asResolver()),
        dep(sum2_s, depInput(MultiAdd_Req.numbers_s).usingAsIs(numbers2_s).asResolver()),
        dep(sum3_s, depInput(MultiAdd_Req.numbers_s).usingAsIs(numbers3_s).asResolver()));
  }

  @Output
  public static ThreeSums output(
      Optional<Integer> sum1, Optional<Integer> sum2, Optional<Integer> sum3) {
    return new ThreeSums(sum1.orElse(0), sum2.orElse(0), sum3.orElse(0));
  }

  public record ThreeSums(int sum1, int sum2, int sum3) {}
}
