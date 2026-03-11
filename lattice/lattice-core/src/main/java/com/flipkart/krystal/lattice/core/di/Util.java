package com.flipkart.krystal.lattice.core.di;

import jakarta.inject.Provider;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class Util {
  public static <T> Optional<T> asOptional(Provider<T> provider) {
    try {
      return Optional.ofNullable(provider.get());
    } catch (Exception e) {
      log.info(
          "jakarta.inject.Provider.get() threw an exception. Converting it to Optional.empty()", e);
      return Optional.empty();
    }
  }
}
