package com.flipkart.krystal.vajram.modulation;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.vajram.inputs.InputValuesAdaptor;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class Batcher implements InputModulator<InputValuesAdaptor, InputValuesAdaptor> {

  private static final int DEFAULT_BATCH_SIZE = 1;
  private final List<
          Consumer<ImmutableList<ModulatedInput<InputValuesAdaptor, InputValuesAdaptor>>>>
      terminationListeners = new ArrayList<>();
  private final Map<InputValuesAdaptor, List<InputValuesAdaptor>> unModulatedRequests =
      new HashMap<>();
  private int minBatchSize = DEFAULT_BATCH_SIZE;

  public Batcher() {}

  public Batcher(int minBatchSize) {
    this.minBatchSize = minBatchSize;
  }

  @Override
  public ImmutableList<ModulatedInput<InputValuesAdaptor, InputValuesAdaptor>> add(
      InputValuesAdaptor inputsNeedingModulation, InputValuesAdaptor commonInputs) {
    unModulatedRequests
        .computeIfAbsent(commonInputs, k -> new ArrayList<>())
        .add(inputsNeedingModulation);
    return getModulatedInputs(commonInputs, false);
  }

  private ImmutableList<ModulatedInput<InputValuesAdaptor, InputValuesAdaptor>> getModulatedInputs(
      InputValuesAdaptor commonInputs, boolean force) {
    ImmutableList<InputValuesAdaptor> inputsNeedingModulations =
        ImmutableList.copyOf(unModulatedRequests.get(commonInputs));
    if (force || inputsNeedingModulations.size() >= minBatchSize) {
      unModulatedRequests.put(commonInputs, new ArrayList<>());
      return ImmutableList.of(new ModulatedInput<>(inputsNeedingModulations, commonInputs));
    }
    return ImmutableList.of();
  }

  @Override
  public void terminate() {
    ImmutableList<ModulatedInput<InputValuesAdaptor, InputValuesAdaptor>> modulatedInputs =
        unModulatedRequests.keySet().stream()
            .map(commonInputs -> getModulatedInputs(commonInputs, true))
            .flatMap(Collection::stream)
            .collect(toImmutableList());
    terminationListeners.forEach(
        modulatedInputConsumer -> modulatedInputConsumer.accept(modulatedInputs));
  }

  @Override
  public void onTermination(
      Consumer<ImmutableList<ModulatedInput<InputValuesAdaptor, InputValuesAdaptor>>> listener) {
    terminationListeners.add(listener);
  }

  @Override
  public void onConfigUpdate(ConfigProvider configProvider) {
    this.minBatchSize =
        configProvider.<Integer>getConfig("min_batch_size").orElse(DEFAULT_BATCH_SIZE);
  }
}
