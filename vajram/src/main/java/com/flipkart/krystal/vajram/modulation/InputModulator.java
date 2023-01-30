package com.flipkart.krystal.vajram.modulation;

import com.flipkart.krystal.config.ConfigListener;
import com.google.common.collect.ImmutableList;
import java.util.function.Consumer;

/**
 * An input modulator has the ability to squash (modulate) multiple inputs into a smaller set of
 * inputs/collection of inputs. Modulation is generally needed when performance optimizations can be
 * achieved by squashing multiple inputs into a single requests. (For example when making I/O calls
 * like network calls/Database queries).
 *
 * <p>Input modulator work by collecting multiple sets of inputs into a collection and "modulate"
 * them by squashing/merging these when some condition is met. For example, {@link Batcher} keeps
 * collecting inputs until a minimum batch size is reached.
 *
 * @param <InputsNeedingModulation> Those inputs which can to be modulated into a single request.
 * @param <CommonInputs> Those inputs which need do not vary within a single request. Meaning, two
 *     requests with differing CommonInputs can never be modulated into a single request.
 */
public interface InputModulator<InputsNeedingModulation, CommonInputs> extends ConfigListener {

  ImmutableList<ModulatedInput<InputsNeedingModulation, CommonInputs>> add(
      InputsNeedingModulation inputsNeedingModulation, CommonInputs commonInputs);

  /** Externally trigger modulation */
  void modulate();

  /**
   * When this InputModulator decides to modulate (due to some internal state like a timer), or when
   * the {@link #modulate()} method is called, execute the given callback.
   */
  void onModulation(
      Consumer<ImmutableList<ModulatedInput<InputsNeedingModulation, CommonInputs>>> callback);
}
