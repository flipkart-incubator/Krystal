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
    return getModulatedInputs(commonInputs);
  }

  private ImmutableList<ModulatedInput<Object, Object>> getModulatedInputs(Object commonInputs) {
    List<Object> inputsNeedingModulations = unModulatedRequests.get(commonInputs);
    if (inputsNeedingModulations.size() >= minBatchSize) {
      unModulatedRequests.put(commonInputs, new ArrayList<>());
      return ImmutableList.of(
          new ModulatedInput<>(ImmutableList.copyOf(inputsNeedingModulations), commonInputs));
    }
    return ImmutableList.of();
  }

  @Override
  public ImmutableList<ModulatedInput<Object, Object>> terminate() {
    return unModulatedRequests.keySet().stream()
        .map(this::getModulatedInputs)
        .flatMap(Collection::stream)
        .collect(toImmutableList());
  }

  @Override
  public void onInternalTermination(Consumer<ModulatedInput<Object, Object>> callback) {
    // This batcher cannot terminate by itself. So these callbacks are not respected.
  }
}
