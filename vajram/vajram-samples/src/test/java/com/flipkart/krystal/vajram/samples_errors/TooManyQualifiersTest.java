package com.flipkart.krystal.vajram.samples_errors;

import static com.flipkart.krystal.vajram.VajramID.ofVajram;
import static com.google.inject.Guice.createInjector;
import static com.google.inject.name.Names.named;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.vajram.MandatoryFacetsMissingException;
import com.flipkart.krystal.vajram.guice.VajramGuiceInjector;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.Builder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TooManyQualifiersTest {

  private Builder graph;

  @BeforeEach
  void setUp() {
    graph =
        new VajramKryonGraph.Builder().loadFromPackage(TooManyQualifiers.class.getPackageName());
  }

  @Test
  void tooManyQualifiersInjectionFacet_throws() {
    CompletableFuture<@Nullable String> result;
    try (VajramKryonGraph vajramKryonGraph = graph.build();
        KrystexVajramExecutor executor = createExecutor(vajramKryonGraph)) {
      result =
          executor.execute(
              ofVajram(TooManyQualifiers.class),
              TooManyQualifiersRequest.builder().input("i1").build());
    }
    assertThat(result)
        .failsWithin(1, SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseInstanceOf(MandatoryFacetsMissingException.class)
        .withMessageContaining(
            "Cause: More than one @jakarta.inject.Qualifier annotations ([@jakarta.inject.Named(\"toInject\"),"
                + " @com.flipkart.krystal.vajram.samples_errors.TooManyQualifiers$InjectionQualifier()]) found on input "
                + "'inject' of vajram 'TooManyQualifiers'. This is not allowed");
  }

  private static KrystexVajramExecutor createExecutor(VajramKryonGraph vajramKryonGraph) {
    return vajramKryonGraph.createExecutor(
        KrystexVajramExecutorConfig.builder()
            .inputInjectionProvider(
                new VajramGuiceInjector(
                    createInjector(
                        binder -> {
                          binder
                              .bind(String.class)
                              .annotatedWith(named("toInject"))
                              .toInstance("i2");
                        })))
            .build());
  }
}
