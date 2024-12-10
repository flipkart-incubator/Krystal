package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.VajramInjectionProvider;
import lombok.Builder;
import lombok.NonNull;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.nullness.qual.Nullable;

public record KrystexVajramExecutorConfig(
    String requestId,
    @NonNull @CalledMethods("singleThreadExecutor")
        KryonExecutorConfigBuilder kryonExecutorConfigBuilder,
    @Nullable VajramInjectionProvider inputInjectionProvider) {

  @Builder
  public KrystexVajramExecutorConfig {}
}
