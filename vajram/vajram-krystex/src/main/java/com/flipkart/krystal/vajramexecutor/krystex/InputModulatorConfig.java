package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.krystex.decoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig.DecoratorContext;
import com.flipkart.krystal.krystex.node.DefaultDependantChain;
import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.DependantChainFirstNode;
import com.flipkart.krystal.krystex.node.DependantChainStart;
import com.flipkart.krystal.krystex.node.NodeDefinitionRegistry;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.InputValuesAdaptor;
import com.flipkart.krystal.vajram.modulation.InputModulator;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.flipkart.krystal.vajram.tags.VajramTags;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public record InputModulatorConfig(
    Supplier<InputModulator<InputValuesAdaptor, InputValuesAdaptor>> inputModulatorSupplier,
    Function<LogicExecutionContext, String> instanceIdGenerator,
    Function<ModulatorContext, MainLogicDecorator> decoratorFactory) {

  /**
   * Creates a default {@link InputModulatorConfig} which guarantees that every unique {@link
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
        inputModulatorSupplier,
        logicExecutionContext ->
            generateInstanceId(
                    logicExecutionContext.dependants(),
                    logicExecutionContext.nodeDefinitionRegistry())
                .toString(),
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
      Set<DependantChain> dependantChains) {
    if (dependantChains.size() < 2) {
      throw new IllegalArgumentException(
          "Share input modulators must be shared across at least two dependant chains. Found %s"
              .formatted(dependantChains.size()));
    }
    return new InputModulatorConfig(
        inputModulatorSupplier,
        logicExecutionContext -> instanceId,
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
      DependantChain dependantChain, NodeDefinitionRegistry nodeDefinitionRegistry) {
    if (dependantChain instanceof DependantChainStart dependantChainStart) {
      return new StringBuilder(dependantChainStart.asString());
    } else {
      if (dependantChain instanceof DependantChainFirstNode dependantChainFirstNode) {
        String vajramId =
            Optional.ofNullable(
                    nodeDefinitionRegistry
                        .logicDefinitionRegistry()
                        .getMain(
                            nodeDefinitionRegistry
                                .get(dependantChainFirstNode.nodeId())
                                .mainLogicNode())
                        .logicTags()
                        .get(VajramTags.VAJRAM_ID))
                .orElseThrow(
                    () ->
                        new NoSuchElementException(
                            "Could not find tag %s for node %s"
                                .formatted(VajramTags.VAJRAM_ID, dependantChainFirstNode.nodeId())))
                .tagValue();
        return generateInstanceId(dependantChainFirstNode.dependantChain(), nodeDefinitionRegistry)
            .append('>')
            .append(vajramId)
            .append(':')
            .append(dependantChainFirstNode.dependencyName());
      } else if (dependantChain instanceof DefaultDependantChain defaultDependantChain) {
        return generateInstanceId(defaultDependantChain.dependantChain(), nodeDefinitionRegistry)
            .append('>')
            .append(defaultDependantChain.dependencyName());
      }
    }
    throw new UnsupportedOperationException();
  }

  public record ModulatorContext(Vajram<?> vajram, DecoratorContext decoratorContext) {}
}
