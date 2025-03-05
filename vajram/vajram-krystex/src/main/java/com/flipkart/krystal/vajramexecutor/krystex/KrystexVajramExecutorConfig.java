package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.traits.TraitBindingProvider;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import lombok.Builder;
import lombok.NonNull;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.nullness.qual.Nullable;

public record KrystexVajramExecutorConfig(
    String requestId,
    @NonNull @CalledMethods("singleThreadExecutor")
        KryonExecutorConfigBuilder kryonExecutorConfigBuilder,
    @Nullable VajramInjectionProvider inputInjectionProvider,
    @Nullable TraitBindingProvider traitBindingProvider) {

  @Builder
  public KrystexVajramExecutorConfig {}
}
