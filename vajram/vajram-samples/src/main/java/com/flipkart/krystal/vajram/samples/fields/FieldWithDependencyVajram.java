package com.flipkart.krystal.vajram.samples.fields;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.samples.fields.FieldWithDependencyVajram_Fac.computedValue_n;
import static com.flipkart.krystal.vajram.samples.fields.FieldWithDependencyVajram_Fac.sum_n;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.ComputeField;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Field;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.samples.calculator.add.Add;
import com.flipkart.krystal.vajram.samples.calculator.add.Add_Req;

/**
 * Test vajram demonstrating fields used in resolvers. Shows how a field can be computed and then
 * used to resolve dependency inputs.
 */
@Vajram
public abstract class FieldWithDependencyVajram extends ComputeVajramDef<String> {

  interface _Inputs {
    @IfAbsent(FAIL)
    int baseValue();

    @IfAbsent(FAIL)
    int increment();
  }

  interface _InternalFacets {
    @IfAbsent(FAIL)
    @Field
    int computedValue();

    @IfAbsent(FAIL)
    @Dependency(onVajram = Add.class)
    int sum();
  }

  @ComputeField(computedValue_n)
  static int computeValue(int baseValue, int increment) {
    return baseValue + increment;
  }

  @Resolve(dep = sum_n, depInputs = Add_Req.numberOne_n)
  static int resolveNumberOne(int computedValue) {
    return computedValue;
  }

  @Resolve(dep = sum_n, depInputs = Add_Req.numberTwo_n)
  static int resolveNumberTwo() {
    return 10;
  }

  @Output
  static String output(int computedValue, int sum) {
    return "Computed: " + computedValue + ", Sum with 10: " + sum;
  }
}
