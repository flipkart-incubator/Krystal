package com.flipkart.krystal.vajram.ext.cdi.injection;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanContainer;
import jakarta.inject.Provider;
import java.lang.reflect.Type;

sealed interface ProviderWrapper permits CloseableProviderCreator, SimpleProvider {

  /**
   * Returns a provider that can be used to inject the bean. For Dependent scoped beans, a new
   * closeable provider is created every time this method is called since these are single use
   * providers.
   *
   * <p>In other cases, the same provider is returned always.
   *
   * @return
   */
  Provider<?> provider();

  static ProviderWrapper newProviderWrapper(Bean<?> bean, Type type, BeanContainer beanContainer) {
    if (bean == null) {
      return new SimpleProvider(() -> null);
    }
    if (bean.getScope().equals(Dependent.class)) {
      // Only @Dependent beans need manual lifecycle management (calling release()/destroy())
      return new CloseableProviderCreator(
          () -> new DependentRefProvider(bean, type, beanContainer));
    } else {
      return new SimpleProvider(
          () ->
              beanContainer.getReference(bean, type, beanContainer.createCreationalContext(bean)));
    }
  }
}
