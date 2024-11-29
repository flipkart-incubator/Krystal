package com.flipkart.krystal.vajram.guice;

import static com.flipkart.krystal.data.Errable.errableFrom;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.except.StackTracelessException;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.facets.InputDef;
import com.flipkart.krystal.vajram.facets.InputSource;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.VajramInjectionProvider;
import com.google.inject.Injector;
import com.google.inject.Key;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VajramGuiceInjector implements VajramInjectionProvider {

  private final Injector injector;
  private final Map<VajramID, Map<String, Provider<?>>> providerCache = new LinkedHashMap<>();

  public VajramGuiceInjector(Injector injector) {
    this.injector = injector;
  }

  @Override
  public <T> Errable<T> get(VajramID vajramID, InputDef<T> inputDef) {
    if (!inputDef.sources().contains(InputSource.SESSION)) {
      return Errable.empty();
    }
    return errableFrom(
        () -> {
          @SuppressWarnings("unchecked")
          Provider<T> provider =
              (Provider<T>)
                  providerCache
                      .computeIfAbsent(vajramID, _v -> new LinkedHashMap<>())
                      .computeIfAbsent(
                          inputDef.name(),
                          _i -> {
                            try {
                              Type type = inputDef.type().javaReflectType();
                              var annotation = getQualifier(vajramID, inputDef);
                              if (annotation.isEmpty()) {
                                return injector.getProvider(Key.get(type));
                              } else {
                                return injector.getProvider(Key.get(type, annotation.get()));
                              }
                            } catch (ClassNotFoundException e) {
                              throw new StackTracelessException(
                                  "Unable to load data type of Input", e);
                            }
                          });
          return provider.get();
        });
  }

  private Optional<Annotation> getQualifier(VajramID vajramID, InputDef<?> inputDef) {
    List<Annotation> qualifierAnnotations =
        inputDef.tags().asCollection().stream()
            .<Annotation>mapMulti(
                (tag, consumer) -> {
                  Annotation annotation = tag.tagValue();
                  boolean isQualifierAnno =
                      annotation.annotationType().getAnnotation(Qualifier.class) != null;
                  if (isQualifierAnno) {
                    consumer.accept(tag.tagValue());
                  }
                })
            .toList();
    if (qualifierAnnotations.isEmpty()) {
      return Optional.empty();
    } else if (qualifierAnnotations.size() == 1) {
      return Optional.ofNullable(qualifierAnnotations.get(0));
    } else {
      throw new IllegalStateException(
          ("More than one @jakarta.inject.Qualifier annotations (%s) found on input '%s' of vajram '%s'."
                  + " This is not allowed")
              .formatted(qualifierAnnotations, inputDef.name(), vajramID.vajramId()));
    }
  }
}
