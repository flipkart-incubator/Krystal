package com.flipkart.krystal.lattice.vajram;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.lattice.core.di.Bindings;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Builder
public record VajramRequestExecutionContext<RespT extends @Nullable Object>(
    @NonNull ImmutableRequest<RespT> vajramRequest,
    @Nullable Bindings requestScopeSeeds,
    @Nullable KryonExecutorConfigBuilder executorConfigBuilder,
    @Nullable @Singular List<RequestInitializer> requestScopeInitializers) {

  @Override
  public KryonExecutorConfigBuilder executorConfigBuilder() {
    if (executorConfigBuilder == null) {
      return KryonExecutorConfig.builder();
    }
    return executorConfigBuilder;
  }

  @Override
  public Bindings requestScopeSeeds() {
    if (requestScopeSeeds == null) {
      return Bindings.builder().build();
    }
    return requestScopeSeeds;
  }

  @Override
  public List<RequestInitializer> requestScopeInitializers() {
    if (requestScopeInitializers == null) {
      return List.of();
    }
    return requestScopeInitializers;
  }
}
