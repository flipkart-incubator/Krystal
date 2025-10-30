package com.flipkart.krystal.krystex;

import static com.flipkart.krystal.krystex.internal.KryonExecutorExecService.THREAD_LOCAL;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;

@UtilityClass
public class KrystalExecutorUtil {

  /** Returns null of the current thread is not executing a krystal executor managed task */
  public static @Nullable KrystalExecutor getExecutorForCurrentThread() {
    return THREAD_LOCAL.get();
  }
}
