package com.flipkart.krystal.krystex.kryon;

import static lombok.AccessLevel.PACKAGE;

import com.flipkart.krystal.core.VajramID;
import lombok.Data;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@Setter(PACKAGE)
public class KryonExecutorExecutionInfo {
  private @Nullable VajramID activeKryon;
}
