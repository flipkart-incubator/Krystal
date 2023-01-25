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
 * @param <InputsNeedingModulation>
 * @param <CommonInputs>
 */
public interface InputModulator<InputsNeedingModulation, CommonInputs> extends ConfigListener {

  ImmutableList<ModulatedInput<InputsNeedingModulation, CommonInputs>> add(
      InputsNeedingModulation inputsNeedingModulation, CommonInputs commonInputs);

  /**
   * Mark the given inputs as needing termination. If all the inputs passed to this modulator are
   * terminated, then this modulator should forcefully terminate.
   */
  void terminate(InputsNeedingModulation inputsNeedingModulation, CommonInputs commonInputs);

  /**
   * When this InputModulator decides to terminate (due to some internal state like a timer), or
   * when the {@link #terminate(Object, Object)} method is called, execute the given callback.
   */
  void onTermination(
      Consumer<ImmutableList<ModulatedInput<InputsNeedingModulation, CommonInputs>>> callback);
}
