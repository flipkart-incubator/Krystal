package com.flipkart.krystal.vajram.modulation;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class Batcher implements InputModulator<Object, Object> {

  private final List<Consumer<ImmutableList<ModulatedInput<Object, Object>>>> terminationListeners =
      new ArrayList<>();
  private final Map<Object, List<Object>> unModulatedRequests = new HashMap<>();
  private final int minBatchSize;

  public Batcher(int minBatchSize) {
    this.minBatchSize = minBatchSize;
  }

  @Override
  public ImmutableList<ModulatedInput<Object, Object>> add(
      Object inputsNeedingModulation, Object commonInputs) {
    unModulatedRequests
        .computeIfAbsent(commonInputs, k -> new ArrayList<>())
        .add(inputsNeedingModulation);
    return getModulatedInputs(commonInputs, false);
  }

  private ImmutableList<ModulatedInput<Object, Object>> getModulatedInputs(
      Object commonInputs, boolean force) {
    ImmutableList<Object> inputsNeedingModulations =
        ImmutableList.copyOf(unModulatedRequests.get(commonInputs));
    if (force || inputsNeedingModulations.size() >= minBatchSize) {
      unModulatedRequests.put(commonInputs, new ArrayList<>());
      return ImmutableList.of(new ModulatedInput<>(inputsNeedingModulations, commonInputs));
    }
    return ImmutableList.of();
  }

  @Override
  public void terminate() {
    ImmutableList<ModulatedInput<Object, Object>> modulatedInputs =
        unModulatedRequests.keySet().stream()
            .map((Object commonInputs) -> getModulatedInputs(commonInputs, true))
            .flatMap(Collection::stream)
            .collect(toImmutableList());
    terminationListeners.forEach(
        modulatedInputConsumer -> modulatedInputConsumer.accept(modulatedInputs));
  }

  @Override
  public void onTermination(Consumer<ImmutableList<ModulatedInput<Object, Object>>> listener) {
    terminationListeners.add(listener);
  }
}
