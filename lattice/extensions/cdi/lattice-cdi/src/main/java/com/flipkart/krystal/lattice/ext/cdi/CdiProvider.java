package com.flipkart.krystal.lattice.ext.cdi;

import static com.flipkart.krystal.data.Errable.errableFrom;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.lattice.core.di.Bindings;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionProvider;
import com.flipkart.krystal.lattice.core.di.InjectionValueProvider;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategy;
import com.flipkart.krystal.vajram.ext.cdi.injection.VajramCdiInjector;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Singleton;
import java.io.Closeable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public final class CdiProvider implements DependencyInjectionProvider {

  private final CDI<Object> currentCDI;

  public CdiProvider() {
    currentCDI = CDI.current();
  }

  @Override
  public <T> void bindToInstance(Class<? extends T> type, T instance) {
    throw new UnsupportedOperationException("CDI doesn't support binding to instance yet");
  }

  @Override
  public <T> void bindInSingleton(Class<T> type) {
    throw new UnsupportedOperationException("CDI doesn't support binding in singleton yet");
  }

  @Override
  public <T> void bind(Class<T> type, Class<? extends T> to) {
    throw new UnsupportedOperationException("CDI doesn't support runtime binding yet.");
  }

  @Override
  public InjectionValueProvider getInjector() {
    return new InjectionValueProvider() {
      @Override
      public <T> Errable<T> getInstance(Class<T> clazz) {
        return errableFrom(() -> currentCDI.select(clazz).get());
      }
    };
  }

  @Override
  public VajramInjectionProvider toVajramInjectionProvider() {
    return new VajramCdiInjector();
  }

  @Override
  public Closeable openRequestScope(Bindings seedMap, ThreadingStrategy threadingStrategy) {
    log.warn("CDI doesn't support opening request scope");
    return () -> {};
  }
}
