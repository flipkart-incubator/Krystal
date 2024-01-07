package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.calculator.Add2And3InputUtil.sumOf2And3_s;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Dependency;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.samples.calculator.adder.Adder;
import com.flipkart.krystal.vajram.samples.calculator.adder.AdderRequest;
import com.google.common.collect.ImmutableCollection;

/**
 * Returns the sum of numbers '2' and '3'
 */
@VajramDef
abstract class Add2And3 extends ComputeVajram<Integer> {
  @Dependency(onVajram = Adder.class)
  private int sumOf2And3;

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            sumOf2And3_s,
            depInput(AdderRequest.numberOne_s).using(() -> 2),
            depInput(AdderRequest.numberTwo_s).using(() -> 3)));
  }

  @VajramLogic
  static int sum(Add2And3InputUtil.Add2And3Inputs facets) {
    return facets.sumOf2And3();
  }
}
