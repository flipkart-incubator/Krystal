package com.flipkart.krystal.vajram.samples.fields;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.samples.fields.ChainedFieldsVajram_Fac.fullName_n;
import static com.flipkart.krystal.vajram.samples.fields.ChainedFieldsVajram_Fac.greeting_n;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Field;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.ComputeField;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Test vajram demonstrating a field computed from another field and an optional input. */
@Vajram
public abstract class ChainedFieldsVajram extends ComputeVajramDef<String> {

  interface _Inputs {
    @IfAbsent(FAIL)
    String firstName();

    @Nullable String middleName();

    @IfAbsent(FAIL)
    String lastName();
  }

  interface _InternalFacets {
    @IfAbsent(FAIL)
    @Field
    String fullName();

    @IfAbsent(FAIL)
    @Field
    String greeting();
  }

  @ComputeField(fullName_n)
  static String computeFullName(String firstName, @Nullable String middleName, String lastName) {
    return middleName == null
        ? firstName + " " + lastName
        : firstName + " " + middleName + " " + lastName;
  }

  @ComputeField(greeting_n)
  static String computeGreeting(String fullName) {
    return "Hello, " + fullName + ". Welcome!";
  }

  @Output
  static String output(String greeting) {
    return greeting;
  }
}
