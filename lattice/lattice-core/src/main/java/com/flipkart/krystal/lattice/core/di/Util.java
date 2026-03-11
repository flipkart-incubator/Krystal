package com.flipkart.krystal.lattice.core.di;

import jakarta.inject.Provider;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;

@UtilityClass
public class Util {
  public static <T> Optional<T> asOptional(@Nullable Provider<T> provider) {
    if (provider == null) {
      return Optional.empty();
    }
    try {
      return Optional.ofNullable(provider.get());
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
