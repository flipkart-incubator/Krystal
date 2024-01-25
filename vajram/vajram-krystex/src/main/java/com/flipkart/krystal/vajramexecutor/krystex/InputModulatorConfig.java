package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.krystex.kryon.DefaultDependantChain;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.DependantChainStart;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig.DecoratorContext;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.InputValuesAdaptor;
import com.flipkart.krystal.vajram.modulation.InputModulator;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.flipkart.krystal.vajram.tags.AnnotationTag;
import com.flipkart.krystal.vajram.tags.NamedValueTag;
import com.flipkart.krystal.vajram.tags.VajramTags;
import com.google.common.collect.ImmutableSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public record InputModulatorConfig(
    Function<LogicExecutionContext, String> instanceIdGenerator,
    Predicate<LogicExecutionContext> shouldModulate,
    Function<ModulatorContext, OutputLogicDecorator> decoratorFactory) {

  /**
   * Creates a default InputModulatorConfig which guarantees that every unique {@link
   * DependantChain} of a vajram gets its own {@link InputModulationDecorator} and its own
   * corresponding {@link InputModulator}. The instance id corresponding to a particular {@link
   * DependantChain} is of the form:
   *
   * <p>{@code [Start]>vajramId_1:dep_1>vajramId_2:dep_2>....>vajramId_n:dep_n}
   *
   * @param inputModulatorSupplier Supplies the {@link InputModulator} corresponding to an {@link
   *     InputModulationDecorator}. This supplier is guaranteed to be called exactly once for every
   *     unique {@link InputModulationDecorator} instance.
   */
  public static InputModulatorConfig simple(
      Supplier<InputModulator<InputValuesAdaptor, InputValuesAdaptor>> inputModulatorSupplier) {
    return new InputModulatorConfig(
        logicExecutionContext ->
            generateInstanceId(
                    logicExecutionContext.dependants(),
                    logicExecutionContext.kryonDefinitionRegistry())
                .toString(),
        _x -> true,
        modulatorContext -> {
          @SuppressWarnings("unchecked")
          var inputsConvertor =
              (InputsConverter<InputValuesAdaptor, InputValuesAdaptor>)
                  modulatorContext.vajram().getInputsConvertor();
          return new InputModulationDecorator<>(
              modulatorContext.decoratorContext().instanceId(),
              inputModulatorSupplier.get(),
              inputsConvertor,
              dependantChain ->
                  modulatorContext
                      .decoratorContext()
                      .logicExecutionContext()
                      .dependants()
                      .equals(dependantChain));
        });
  }

  public static InputModulatorConfig sharedModulator(
      Supplier<InputModulator<InputValuesAdaptor, InputValuesAdaptor>> inputModulatorSupplier,
      String instanceId,
      DependantChain... dependantChains) {
    return sharedModulator(
        inputModulatorSupplier, instanceId, ImmutableSet.copyOf(dependantChains));
  }

  public static InputModulatorConfig sharedModulator(
      Supplier<InputModulator<InputValuesAdaptor, InputValuesAdaptor>> inputModulatorSupplier,
      String instanceId,
      ImmutableSet<DependantChain> dependantChains) {
    return new InputModulatorConfig(
        logicExecutionContext -> instanceId,
        logicExecutionContext -> dependantChains.contains(logicExecutionContext.dependants()),
        modulatorContext -> {
          @SuppressWarnings("unchecked")
          var inputsConvertor =
              (InputsConverter<InputValuesAdaptor, InputValuesAdaptor>)
                  modulatorContext.vajram().getInputsConvertor();
          return new InputModulationDecorator<>(
              instanceId, inputModulatorSupplier.get(), inputsConvertor, dependantChains::contains);
        });
  }

  /**
   * @return decorator instanceId of the form {@code
   *     [Start]>vajramId_1:dep_1>vajramId_2:dep_2>....>vajramId_n:dep_n}
   */
  private static StringBuilder generateInstanceId(
      DependantChain dependantChain, KryonDefinitionRegistry kryonDefinitionRegistry) {
    if (dependantChain instanceof DependantChainStart dependantChainStart) {
      return new StringBuilder(dependantChainStart.toString());
    } else if (dependantChain instanceof DefaultDependantChain defaultDependantChain) {
      if (defaultDependantChain.dependantChain() instanceof DependantChainStart) {
        String vajramId =
            Optional.ofNullable(
                    kryonDefinitionRegistry
                        .get(defaultDependantChain.kryonId())
                        .getOutputLogicDefinition()
                        .logicTags()
                        .get(VajramTags.VAJRAM_ID))
                .map(
                    tag -> {
                      if (tag instanceof AnnotationTag<?> anno) {
                        if (anno.annotation() instanceof NamedValueTag namedValueTag) {
                          return namedValueTag.value();
                        }
                      }
                      return null;
                    })
                .orElseThrow(
                    () ->
                        new NoSuchElementException(
                            "Could not find tag %s for kryon %s"
                                .formatted(VajramTags.VAJRAM_ID, defaultDependantChain.kryonId())));
        return generateInstanceId(defaultDependantChain.dependantChain(), kryonDefinitionRegistry)
            .append('>')
            .append(vajramId)
            .append(':')
            .append(defaultDependantChain.dependencyName());
      } else {
        return generateInstanceId(defaultDependantChain.dependantChain(), kryonDefinitionRegistry)
            .append('>')
            .append(defaultDependantChain.dependencyName());
      }
    }
    throw new UnsupportedOperationException();
  }

  public record ModulatorContext(Vajram<?> vajram, DecoratorContext decoratorContext) {}
}
