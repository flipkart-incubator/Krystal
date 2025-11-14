package com.flipkart.krystal.vajramexecutor.krystex.traits;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;

@UtilityClass
public class DispatchTargetComputing {
  public sealed interface DispatchTargetComputer<T extends Request<?>> {
    @Nullable Object computeDispatchTarget(@Nullable Dependency dependency, T request);
  }

  @FunctionalInterface
  public non-sealed interface DispatchTargetIdComputer<T extends Request<?>>
      extends DispatchTargetComputer<T> {
    @Nullable VajramID computeDispatchTarget(@Nullable Dependency dependency, T request);
  }

  @FunctionalInterface
  public non-sealed interface DispatchTargetReqTypeComputer<T extends Request<?>>
      extends DispatchTargetComputer<T> {
    @Nullable Class<? extends T> computeDispatchTarget(@Nullable Dependency dependency, T request);
  }
}
