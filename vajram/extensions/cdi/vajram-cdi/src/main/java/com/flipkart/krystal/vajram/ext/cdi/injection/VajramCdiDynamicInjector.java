package com.flipkart.krystal.vajram.ext.cdi.injection;

import static com.flipkart.krystal.facets.FacetType.INJECTION;
import static com.flipkart.krystal.vajram.ext.cdi.injection.ProviderWrapper.newProviderWrapper;
import static com.flipkart.krystal.vajram.inputinjection.InputInjectionUtils.getQualifiers;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.except.KrystalCompletionException;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanContainer;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Provider;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

public class VajramCdiDynamicInjector implements VajramInjectionProvider {

  private final Map<VajramID, Map<Facet, ProviderWrapper>> providerCache = new LinkedHashMap<>();
  private final BeanContainer beanContainer;

  public VajramCdiDynamicInjector() {
    this.beanContainer = CDI.current().getBeanContainer();
  }

  @Override
  public <T> Provider<T> get(VajramID vajramID, FacetSpec<T, ?> facetDef) {
    if (!INJECTION.equals(facetDef.facetType())) {
      throw new IllegalArgumentException("Facet type of %s is not INJECTION".formatted(facetDef));
    }
    @SuppressWarnings("unchecked")
    Provider<T> provider =
        (Provider<T>)
            providerCache
                .computeIfAbsent(vajramID, _v -> new LinkedHashMap<>())
                .computeIfAbsent(
                    facetDef,
                    _i -> {
                      Type type;
                      try {
                        type = facetDef.type().javaReflectType();
                      } catch (ClassNotFoundException e) {
                        throw new KrystalCompletionException(
                            "Unable to load data type of Input", e);
                      }
                      Bean<?> bean =
                          beanContainer.resolve(
                              beanContainer.getBeans(type, getQualifiers(facetDef)));
                      return newProviderWrapper(bean, type, beanContainer);
                    })
                .provider();
    return provider;
  }
}
