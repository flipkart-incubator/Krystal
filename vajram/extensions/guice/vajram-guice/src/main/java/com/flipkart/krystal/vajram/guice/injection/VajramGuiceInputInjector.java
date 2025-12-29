package com.flipkart.krystal.vajram.guice.injection;

import static com.flipkart.krystal.facets.FacetType.INJECTION;
import static com.flipkart.krystal.vajram.inputinjection.InputInjectionUtils.getQualifiers;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.except.KrystalException;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import com.google.inject.Injector;
import com.google.inject.Key;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VajramGuiceInputInjector implements VajramInjectionProvider {

  private final Injector injector;
  private final Map<VajramID, Map<String, Provider<?>>> providerCache = new LinkedHashMap<>();

  @Inject
  public VajramGuiceInputInjector(Injector injector) {
    this.injector = injector;
  }

  @Override
  public <T> Provider<T> get(VajramID vajramID, FacetSpec<T, ?> facetDef) {
    if (!INJECTION.equals(facetDef.facetType())) {
      return () -> null;
    }
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
                        var annotation = getQualifier(vajramID, facetDef);
                        if (annotation.isEmpty()) {
                          return injector.getProvider(Key.get(type));
                        } else {
                          return injector.getProvider(Key.get(type, annotation.get()));
                        }
                      } catch (ClassNotFoundException e) {
                        throw new KrystalException("Unable to load data type of Input", e);
                      }
                    });
    return provider;
  }

  private <T> Optional<Annotation> getQualifier(VajramID vajramID, FacetSpec<T, ?> facetDef) {
    Annotation[] qualifierAnnotations = getQualifiers(facetDef);
    if (qualifierAnnotations.length == 0) {
      return Optional.empty();
    } else if (qualifierAnnotations.length == 1) {
      return Optional.ofNullable(qualifierAnnotations[0]);
    } else {
      throw new IllegalStateException(
          ("More than one @jakarta.inject.Qualifier annotations (%s) found on input '%s' of vajram '%s'."
                  + " This is not allowed")
              .formatted(qualifierAnnotations, facetDef.name(), vajramID));
    }
  }
}
