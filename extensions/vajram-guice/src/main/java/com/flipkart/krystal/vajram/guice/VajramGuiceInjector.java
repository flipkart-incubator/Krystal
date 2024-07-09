package com.flipkart.krystal.vajram.guice;

import static com.flipkart.krystal.data.Errable.errableFrom;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.except.StackTracelessException;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.facets.InputDef;
import com.flipkart.krystal.vajram.facets.InputSource;
import com.flipkart.krystal.vajram.tags.AnnotationTag;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.VajramInjectionProvider;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import com.google.inject.Key;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public class VajramGuiceInjector implements VajramInjectionProvider {
  private final VajramKryonGraph vajramKryonGraph;
  private final Injector injector;
  private final Map<VajramID, Map<String, Provider<?>>> providerCache = new LinkedHashMap<>();

  public VajramGuiceInjector(VajramKryonGraph vajramKryonGraph, Injector injector) {
    this.vajramKryonGraph = vajramKryonGraph;
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
                              if (annotation == null) {
                                return injector.getProvider(Key.get(type));
                              } else {
                                return injector.getProvider(Key.get(type, annotation));
                              }
                            } catch (ClassNotFoundException e) {
                              throw new StackTracelessException(
                                  "Unable to load data type of Input", e);
                            }
                          });
          return provider.get();
        });
  }

  private @Nullable Annotation getQualifier(VajramID vajramID, InputDef<?> inputDef) {
    Optional<VajramDefinition> vajramDefinition = vajramKryonGraph.getVajramDefinition(vajramID);
    if (vajramDefinition.isEmpty()) {
      return null;
    }
    ImmutableMap<String, ImmutableMap<Object, Tag>> facetTags =
        vajramDefinition.get().getFacetTags();

    String inputName = inputDef.name();
    return Optional.ofNullable(
            facetTags.getOrDefault(inputName, ImmutableMap.of()).get(Named.class))
        .map(
            tag -> {
              if (tag instanceof AnnotationTag<?> annoTag
                  && annoTag.tagValue() instanceof Named named) {
                return named;
              }
              return null;
            })
        .orElse(null);
  }
}
