package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

@ToString
public class KrystalExecutorExecutionInfo {
  private @Nullable VajramID activeVajram;

  KrystalExecutorExecutionInfo activeVajram(@Nullable VajramID activeVajram) {
    this.activeVajram = activeVajram;
    return this;
  }

  public @Nullable VajramID activeVajram() {
    return this.activeVajram;
  }
}
