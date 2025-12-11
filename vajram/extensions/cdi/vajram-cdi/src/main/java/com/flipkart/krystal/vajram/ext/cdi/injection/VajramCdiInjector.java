package com.flipkart.krystal.vajram.ext.cdi.injection;

import static com.flipkart.krystal.data.Errable.errableFrom;
import static com.flipkart.krystal.data.Errable.nil;
import static com.flipkart.krystal.facets.FacetType.INJECTION;
import static com.flipkart.krystal.vajram.inputinjection.InputInjectionUtils.getQualifiers;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.except.StackTracelessException;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Provider;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

public class VajramCdiInjector implements VajramInjectionProvider {

  private final Map<VajramID, Map<String, Provider<?>>> providerCache = new LinkedHashMap<>();
  private final BeanManager beanManager;
  private final CDI<Object> currentCDI;

  public VajramCdiInjector() {
    this.currentCDI = CDI.current();
    this.beanManager = currentCDI.getBeanManager();
  }

  @Override
  public <T> Errable<T> get(VajramID vajramID, FacetSpec<T, ?> facetDef) {
    if (!INJECTION.equals(facetDef.facetType())) {
      return nil();
    }
    return errableFrom(
        () -> {
          @SuppressWarnings("unchecked")
          Provider<T> provider =
              (Provider<T>)
                  providerCache
                      .computeIfAbsent(vajramID, _v -> new LinkedHashMap<>())
                      .computeIfAbsent(
                          facetDef.name(),
                          _i -> {
                            try {
                              Type type = facetDef.type().javaReflectType();
                              var qualifiers = getQualifiers(facetDef);
                              if (type instanceof Class<?> clazz) {
                                return currentCDI.select(clazz, qualifiers);
                              } else {
                                return () -> {
                                  Bean<?> bean =
                                      beanManager.resolve(beanManager.getBeans(type, qualifiers));
                                  if (bean == null) {
                                    return null;
                                  }
                                  return beanManager.getReference(
                                      bean, type, beanManager.createCreationalContext(bean));
                                };
                              }
                            } catch (ClassNotFoundException e) {
                              throw new StackTracelessException(
                                  "Unable to load data type of Input", e);
                            }
                          });
          return provider.get();
        });
  }
}
