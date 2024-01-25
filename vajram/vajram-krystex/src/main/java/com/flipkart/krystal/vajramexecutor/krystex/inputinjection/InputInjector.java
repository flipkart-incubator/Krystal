package com.flipkart.krystal.vajramexecutor.krystex.inputinjection;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.InputSource;
import com.flipkart.krystal.vajram.facets.VajramFacetDefinition;
import com.flipkart.krystal.vajram.tags.AnnotationTag;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import jakarta.inject.Named;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputInjector implements OutputLogicDecorator {

  public static final String DECORATOR_TYPE = InputInjector.class.getName();
  @NotOnlyInitialized private final VajramKryonGraph vajramKryonGraph;
  private final @Nullable InputInjectionProvider inputInjectionProvider;

  public InputInjector(
      @UnderInitialization VajramKryonGraph vajramKryonGraph,
      @Nullable InputInjectionProvider inputInjectionProvider) {
    this.vajramKryonGraph = vajramKryonGraph;
    this.inputInjectionProvider = inputInjectionProvider;
  }

  @Override
  public OutputLogic<Object> decorateLogic(
      OutputLogic<Object> logicToDecorate, OutputLogicDefinition<Object> originalLogicDefinition) {
    return inputsList -> {
      Map<Inputs, Inputs> newInputsToOldInputs = new HashMap<>();
      ImmutableList<Inputs> inputValues =
          inputsList.stream()
              .map(
                  inputs -> {
                    Inputs newInputs =
                        injectFromSession(
                            vajramKryonGraph
                                .getVajramDefinition(
                                    VajramID.vajramID(
                                        Optional.ofNullable(originalLogicDefinition.kryonLogicId())
                                            .map(KryonLogicId::kryonId)
                                            .map(KryonId::value)
                                            .orElse("")))
                                .orElse(null),
                            inputs);
                    newInputsToOldInputs.put(newInputs, inputs);
                    return newInputs;
                  })
              .collect(toImmutableList());

      ImmutableMap<Inputs, CompletableFuture<@Nullable Object>> result =
          logicToDecorate.execute(inputValues);

      // Change the Map key back to the original Inputs list as SESSION inputs were injected
      return result.entrySet().stream()
          .collect(
              toImmutableMap(
                  e -> newInputsToOldInputs.getOrDefault(e.getKey(), e.getKey()), Entry::getValue));
    };
  }

  @Override
  public String getId() {
    return InputInjector.class.getName();
  }

  private Inputs injectFromSession(@Nullable VajramDefinition vajramDefinition, Inputs inputs) {
    Map<String, InputValue<Object>> newValues = new HashMap<>();
    ImmutableMap<String, ImmutableMap<Object, Tag>> facetTags =
        vajramDefinition == null ? ImmutableMap.of() : vajramDefinition.getFacetTags();
    Optional.ofNullable(vajramDefinition)
        .map(VajramDefinition::getVajram)
        .map(Vajram::getFacetDefinitions)
        .ifPresent(
            facetDefinitions -> {
              for (VajramFacetDefinition facetDefinition : facetDefinitions) {
                String inputName = facetDefinition.name();
                if (facetDefinition instanceof Input<?> input) {
                  if (input.sources().contains(InputSource.CLIENT)) {
                    ValueOrError<Object> value = inputs.getInputValue(inputName);
                    if (!ValueOrError.empty().equals(value)) {
                      continue;
                    }
                    // Input was not resolved by another vajram. Check if it is resolvable
                    // by SESSION
                  }
                  if (input.sources().contains(InputSource.SESSION)) {
                    ImmutableMap<Object, Tag> inputTags =
                        facetTags.getOrDefault(inputName, ImmutableMap.of());
                    ValueOrError<Object> value =
                        getFromInjectionAdaptor(
                            input.type(),
                            Optional.ofNullable(inputTags.get(Named.class))
                                .map(
                                    tag -> {
                                      if (tag instanceof AnnotationTag<?> anno) {
                                        if (anno.annotation() instanceof Named named) {
                                          return named.value();
                                        }
                                      }
                                      return null;
                                    })
                                .orElse(null));
                    newValues.put(inputName, value);
                  }
                }
              }
            });
    if (!newValues.isEmpty()) {
      inputs.values().forEach(newValues::putIfAbsent);
      return new Inputs(newValues);
    } else {
      return inputs;
    }
  }

  private ValueOrError<Object> getFromInjectionAdaptor(
      DataType<?> dataType, @Nullable String injectionName) {
    if (inputInjectionProvider == null) {
      return ValueOrError.withError(
          new Exception("Dependency injector is null, cannot resolve SESSION input"));
    }

    if (dataType == null) {
      return ValueOrError.withError(new Exception("Data type not found"));
    }
    Type type;
    try {
      type = dataType.javaReflectType();
    } catch (ClassNotFoundException e) {
      return ValueOrError.withError(e);
    }
    @Nullable Object resolvedObject = null;
    if (injectionName != null) {
      resolvedObject = inputInjectionProvider.getInstance((Class<?>) type, injectionName);
    }
    if (resolvedObject == null) {
      resolvedObject = inputInjectionProvider.getInstance(((Class<?>) type));
    }
    return ValueOrError.withValue(resolvedObject);
  }
}
