package com.flipkart.krystal.vajram.ext.cdi.injection;

import com.flipkart.krystal.vajram.inputinjection.CloseableProvider;
import jakarta.inject.Provider;
import java.util.function.Supplier;

public record CloseableProviderCreator(Supplier<CloseableProvider> creator)
    implements ProviderWrapper {
  @Override
  public Provider<?> provider() {
    return creator.get();
  }
}
