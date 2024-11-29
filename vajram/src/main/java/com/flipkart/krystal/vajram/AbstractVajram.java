package com.flipkart.krystal.vajram;

import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.Vajrams.getVajramSourceClass;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

abstract sealed class AbstractVajram<T> implements Vajram<T> permits ComputeVajram, IOVajram {

  private @MonotonicNonNull VajramID id;

  @Override
  public final VajramID getId() {
    if (id == null) {
      Class<?> vajramSourceClass = getVajramSourceClass(getClass());
      id =
          vajramID(
              Optional.ofNullable(vajramSourceClass.getAnnotation(VajramDef.class))
                  .map(VajramDef::id)
                  .filter(id -> !id.isEmpty())
                  // Empty id means infer vajram id from class name
                  .orElseGet(vajramSourceClass::getSimpleName));
    }
    return id;
  }
}
