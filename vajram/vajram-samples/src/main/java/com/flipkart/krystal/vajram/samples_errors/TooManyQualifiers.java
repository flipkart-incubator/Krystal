package com.flipkart.krystal.vajram.samples_errors;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.Output;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@ExternalInvocation(allow = true)
@Vajram
public abstract class TooManyQualifiers extends ComputeVajramDef<String> {
  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  public @interface InjectionQualifier {}

  @SuppressWarnings("initialization.field.uninitialized")
  static class _Facets {
    @Mandatory @Input String input;

    @Mandatory
    @Inject
    @Named("toInject")
    @InjectionQualifier
    String inject;
  }

  @Output
  static String output(String input, String inject) {
    return input + ' ' + inject;
  }
}
