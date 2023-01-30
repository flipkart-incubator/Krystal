package com.flipkart.krystal.vajram.modulation;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.config.ConfigProvider;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class Batcher<I, C> implements InputModulator<I, C> {

  private static final int DEFAULT_BATCH_SIZE = 1;
  private Consumer<ImmutableList<ModulatedInput<I, C>>> modulationListener;
  private final Map<C, List<I>> unModulatedRequests = new HashMap<>();
  private int minBatchSize = DEFAULT_BATCH_SIZE;

  public Batcher() {}

  public Batcher(int minBatchSize) {
    this.minBatchSize = minBatchSize;
  }

  @Override
  public ImmutableList<ModulatedInput<I, C>> add(I inputsNeedingModulation, C commonInputs) {
    unModulatedRequests
        .computeIfAbsent(commonInputs, k -> new ArrayList<>())
        .add(inputsNeedingModulation);
    return getModulatedInputs(commonInputs, false);
  }

  private ImmutableList<ModulatedInput<I, C>> getModulatedInputs(C commonInputs, boolean force) {
    ImmutableList<I> inputsNeedingModulations =
        ImmutableList.copyOf(unModulatedRequests.get(commonInputs));
    if (force || inputsNeedingModulations.size() >= minBatchSize) {
      unModulatedRequests.put(commonInputs, new ArrayList<>());
      return ImmutableList.of(new ModulatedInput<>(inputsNeedingModulations, commonInputs));
    }
    return ImmutableList.of();
  }

  @Override
  public void modulate() {
    ImmutableList<ModulatedInput<I, C>> modulatedInputs =
        unModulatedRequests.keySet().stream()
            .map(c -> getModulatedInputs(c, true))
            .flatMap(Collection::stream)
            .collect(toImmutableList());
    if (modulationListener != null) {
      modulationListener.accept(modulatedInputs);
    }
  }

  @Override
  public void onModulation(Consumer<ImmutableList<ModulatedInput<I, C>>> listener) {
    modulationListener = listener;
  }

  @Override
  public void onConfigUpdate(ConfigProvider configProvider) {
    this.minBatchSize =
        configProvider.<Integer>getConfig("min_batch_size").orElse(DEFAULT_BATCH_SIZE);
  }
}
