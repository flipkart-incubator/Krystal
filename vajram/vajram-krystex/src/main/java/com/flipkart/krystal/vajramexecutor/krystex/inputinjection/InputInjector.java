package com.flipkart.krystal.vajramexecutor.krystex.inputinjection;

import static com.flipkart.krystal.vajramexecutor.krystex.inputinjection.KryonInputInjector.injectFromSession;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

@Deprecated(since = "6.0.4", forRemoval = true)
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
      Map<Facets, Facets> newInputsToOldInputs = new HashMap<>();
      ImmutableList<Facets> inputValues =
          inputsList.stream()
              .map(
                  inputs -> {
                    Facets newFacets =
                        injectFromSession(
                            vajramKryonGraph
                                .getVajramDefinition(
                                    VajramID.vajramID(
                                        Optional.ofNullable(originalLogicDefinition.kryonLogicId())
                                            .map(KryonLogicId::kryonId)
                                            .map(KryonId::value)
                                            .orElse("")))
                                .orElse(null),
                            inputs,
                            inputInjectionProvider);
                    newInputsToOldInputs.put(newFacets, inputs);
                    return newFacets;
                  })
              .collect(toImmutableList());

      ImmutableMap<Facets, CompletableFuture<@Nullable Object>> result =
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
}
