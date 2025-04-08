package com.flipkart.krystal.vajram.samples_errors;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.data.IfNoValue;
import com.flipkart.krystal.vajram.facets.Output;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@ExternallyInvocable
@Vajram
public abstract class TooManyQualifiers extends ComputeVajramDef<String> {
  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  public @interface InjectionQualifier {}

  @SuppressWarnings("initialization.field.uninitialized")
  static class _Facets {
    @IfNoValue @Input String input;

    @IfNoValue
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
