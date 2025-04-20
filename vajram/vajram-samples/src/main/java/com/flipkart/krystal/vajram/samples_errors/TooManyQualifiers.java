package com.flipkart.krystal.vajram.samples_errors;

import static com.flipkart.krystal.data.IfNull.IfNullThen.FAIL;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SuppressWarnings("initialization.field.uninitialized")
@ExternallyInvocable
@Vajram
public abstract class TooManyQualifiers extends ComputeVajramDef<String> {

  static class _Inputs {
    @IfNull(FAIL)
    String input;
  }

  static class _InternalFacets {
    @IfNull(FAIL)
    @Inject
    @Named("toInject")
    @InjectionQualifier
    String inject;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  public @interface InjectionQualifier {}

  @Output
  static String output(String input, String inject) {
    return input + ' ' + inject;
  }
}
