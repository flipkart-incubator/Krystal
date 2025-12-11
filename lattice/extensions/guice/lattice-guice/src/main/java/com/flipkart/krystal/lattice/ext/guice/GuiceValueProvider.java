package com.flipkart.krystal.lattice.ext.guice;

import static com.flipkart.krystal.data.Errable.errableFrom;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.lattice.core.di.InjectionValueProvider;
import com.google.inject.Injector;

public record GuiceValueProvider(Injector injector) implements InjectionValueProvider {

  @Override
  public <T> Errable<T> getInstance(Class<T> clazz) {
    return errableFrom(() -> injector.getInstance(clazz));
  }
}
