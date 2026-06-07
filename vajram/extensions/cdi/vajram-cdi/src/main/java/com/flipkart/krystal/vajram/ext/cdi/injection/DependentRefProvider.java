package com.flipkart.krystal.vajram.ext.cdi.injection;

import com.flipkart.krystal.vajram.inputinjection.CloseableProvider;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanContainer;
import java.lang.reflect.Type;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class DependentRefProvider implements CloseableProvider {

  private boolean closed;

  private final BeanContainer beanContainer;
  private final Bean<?> bean;
  private final Type type;
  private final CreationalContext<?> creationalContext;

  DependentRefProvider(Bean<?> bean, Type type, BeanContainer beanContainer) {
    this.bean = bean;
    this.type = type;
    this.beanContainer = beanContainer;
    this.creationalContext = beanContainer.createCreationalContext(bean);
  }

  @Override
  public Object get() {
    if (closed) {
      throw new IllegalStateException("DependentRefProvider has already been closed");
    }
    return beanContainer.getReference(bean, type, creationalContext);
  }

  @Override
  public void close() {
    try {
      creationalContext.release();
    } catch (Exception e) {
      log.error(
          "Exception thrown while releasing creationalContext for injection of type {} for bean {}",
          type,
          bean,
          e);
    }
    closed = true;
  }
}
