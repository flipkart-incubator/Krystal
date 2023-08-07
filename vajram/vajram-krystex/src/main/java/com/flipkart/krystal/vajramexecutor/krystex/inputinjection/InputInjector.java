package com.flipkart.krystal.vajramexecutor.krystex.inputinjection;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.datatypes.JavaDataType;
import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputSource;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputInjector implements MainLogicDecorator {

  public static final String DECORATOR_TYPE = InputInjector.class.getName();
  public static final String INJECT_NAMED_KEY = "inject.named";
  @NotOnlyInitialized private final VajramKryonGraph vajramKryonGraph;
  private final @Nullable InputInjectionProvider inputInjectionProvider;

  public InputInjector(
      @UnderInitialization VajramKryonGraph vajramKryonGraph,
      @Nullable InputInjectionProvider inputInjectionProvider) {
    this.vajramKryonGraph = vajramKryonGraph;
    this.inputInjectionProvider = inputInjectionProvider;
  }

  @Override
  public MainLogic<Object> decorateLogic(
      MainLogic<Object> logicToDecorate, MainLogicDefinition<Object> originalLogicDefinition) {
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
    Optional.ofNullable(vajramDefinition)
        .map(VajramDefinition::getVajram)
        .map(Vajram::getInputDefinitions)
        .ifPresent(
            inputDefinitions -> {
              for (VajramInputDefinition inputDefinition : inputDefinitions) {
                String inputName = inputDefinition.name();
                if (inputDefinition instanceof Input<?> input) {
                  if (input.sources().contains(InputSource.CLIENT)) {
                    ValueOrError<Object> value = inputs.getInputValue(inputName);
                    if (!ValueOrError.empty().equals(value)) {
                      continue;
                    }
                    // Input was not resolved by another vajram. Check if it is resolvable
                    // by SESSION
                  }
                  if (input.sources().contains(InputSource.SESSION)
                      && input.type() instanceof JavaDataType<?>) {
                    ValueOrError<Object> value =
                        getFromInjectionAdaptor(
                            ((JavaDataType<?>) input.type()),
                            Optional.ofNullable(input.tags())
                                .map(tags -> tags.get(INJECT_NAMED_KEY))
                                .map(Tag::tagValue)
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
      JavaDataType<?> dataType, @Nullable String injectionName) {
    if (inputInjectionProvider == null) {
      return ValueOrError.withError(
          new Exception("Dependency injector is null, cannot resolve SESSION input"));
    }

    if (dataType == null || dataType.javaType().isEmpty()) {
      return ValueOrError.withError(new Exception("Data type not found"));
    }
    Optional<Type> type = dataType.javaType();
    @Nullable Object resolvedObject = null;
    if (type.isPresent()) {
      if (injectionName != null) {
        resolvedObject = inputInjectionProvider.getInstance((Class<?>) type.get(), injectionName);
      }
      if (resolvedObject == null) {
        resolvedObject = inputInjectionProvider.getInstance(((Class<?>) type.get()));
      }
    }
    return ValueOrError.withValue(resolvedObject);
  }
}
