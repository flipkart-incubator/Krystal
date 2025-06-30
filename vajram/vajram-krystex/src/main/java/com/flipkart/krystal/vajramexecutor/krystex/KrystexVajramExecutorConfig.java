package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import lombok.Builder;
import lombok.NonNull;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;

public record KrystexVajramExecutorConfig(
    @NonNull @CalledMethods("executorService")
        KryonExecutorConfigBuilder kryonExecutorConfigBuilder) {

  @Builder
  public KrystexVajramExecutorConfig {}
}
