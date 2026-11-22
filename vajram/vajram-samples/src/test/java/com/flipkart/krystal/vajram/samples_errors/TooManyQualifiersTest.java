package com.flipkart.krystal.vajram.samples_errors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.vajram.exception.VajramDefinitionException;
import org.junit.jupiter.api.Test;

class TooManyQualifiersTest {

  @Test
  void tooManyQualifiersInjectionFacet_failsGraphLoading() {
    assertThatThrownBy(
            () ->
                VajramGraph.builder()
                    .loadFromPackage(TooManyQualifiers.class.getPackageName())
                    .build())
        .isInstanceOf(VajramDefinitionException.class)
        .hasMessageContaining("inject")
        .hasMessageContaining("TooManyQualifiers")
        .hasMessageContaining("@jakarta.inject.Qualifier");
  }
}
