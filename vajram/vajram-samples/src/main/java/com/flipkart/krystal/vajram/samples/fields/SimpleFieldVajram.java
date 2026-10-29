package com.flipkart.krystal.vajram.samples.fields;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.samples.fields.SimpleFieldVajram_Fac.fullName_n;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Field;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.ComputeField;

/**
 * Test vajram demonstrating simple field computation. Computes fullName from firstName and lastName
 * inputs.
 */
@Vajram
public abstract class SimpleFieldVajram extends ComputeVajramDef<String> {

  interface _Inputs {
    @IfAbsent(FAIL)
    String firstName();

    @IfAbsent(FAIL)
    String lastName();
  }

  interface _InternalFacets {
    @IfAbsent(FAIL)
    @Field
    String fullName();
  }

  @ComputeField(fullName_n)
  static String computeFullName(String firstName, String lastName) {
    return firstName + " " + lastName;
  }

  @Output
  static String output(String fullName) {
    return "Hello, " + fullName + "!";
  }
}
