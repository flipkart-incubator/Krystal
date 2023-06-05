package com.flipkart.krystal.vajramexecutor.krystex.inputinjection;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.datatypes.JavaDataType;
import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputSource;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class InputInjector implements MainLogicDecorator {

  public static final String DECORATOR_TYPE = InputInjector.class.getName();
  public static final String INJECT_NAMED_KEY = "inject.named";
  private VajramNodeGraph vajramNodeGraph;
  private final InputInjectionProvider inputInjectionProvider;

  public InputInjector(
      VajramNodeGraph vajramNodeGraph, InputInjectionProvider inputInjectionProvider) {
    this.vajramNodeGraph = vajramNodeGraph;
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
                            vajramNodeGraph
                                .getVajramDefinition(
                                    VajramID.vajramID(
                                        Optional.ofNullable(originalLogicDefinition.nodeLogicId())
                                            .map(NodeLogicId::nodeId)
                                            .map(NodeId::value)
                                            .orElse("")))
                                .orElse(null),
                            inputs);
                    newInputsToOldInputs.put(newInputs, inputs);
                    return newInputs;
                  })
              .collect(toImmutableList());

      ImmutableMap<Inputs, CompletableFuture<Object>> result = logicToDecorate.execute(inputValues);

      // Change the Map key back to the original Inputs list as SESSION inputs were injected
      Map<Inputs, CompletableFuture<Object>> newResult = new HashMap<>();
      result.forEach(
          (key, value) -> {
            newResult.put(newInputsToOldInputs.getOrDefault(key, key), value);
          });
      return ImmutableMap.copyOf(newResult);
    };
  }

  @Override
  public String getId() {
    return InputInjector.class.getName();
  }

  private Inputs injectFromSession(VajramDefinition vajramDefinition, Inputs inputs) {
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
      JavaDataType<?> dataType, String injectionName) {
    if (inputInjectionProvider == null) {
      return ValueOrError.withError(
          new Exception("Dependency injector is null, cannot resolve SESSION input"));
    }

    if (dataType == null || dataType.javaType().isEmpty()) {
      return ValueOrError.withError(new Exception("Data type not found"));
    }
    Optional<Type> type = dataType.javaType();
    Object resolvedObject = null;
    if (injectionName != null) {
      resolvedObject = inputInjectionProvider.getInstance((Class<?>) type.get(), injectionName);
    }
    if (resolvedObject == null) {
      resolvedObject = inputInjectionProvider.getInstance(((Class<?>) type.get()));
    }
    return ValueOrError.withValue(resolvedObject);
  }
}
