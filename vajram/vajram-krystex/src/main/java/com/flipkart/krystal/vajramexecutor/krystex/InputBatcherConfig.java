package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.kryon.DefaultDependantChain;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.DependantChainStart;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig.DecoratorContext;
import com.flipkart.krystal.vajram.BatchableVajram;
import com.flipkart.krystal.vajram.batching.BatchableSupplier;
import com.flipkart.krystal.vajram.batching.InputBatcher;
import com.flipkart.krystal.vajram.tags.AnnotationTags;
import com.flipkart.krystal.vajram.tags.VajramTags;
import com.google.common.collect.ImmutableSet;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public record InputBatcherConfig(
    Function<LogicExecutionContext, String> instanceIdGenerator,
    Predicate<BatcherContext> shouldBatch,
    Function<BatcherContext, OutputLogicDecorator> decoratorFactory) {

  /**
   * Creates a default InputBatcherConfig which guarantees that every unique {@link DependantChain}
   * of a vajram gets its own {@link InputBatchingDecorator} and its own corresponding {@link
   * InputBatcher}. The instance id corresponding to a particular {@link DependantChain} is of the
   * form:
   *
   * <p>{@code [Start]>vajramId_1:dep_1>vajramId_2:dep_2>....>vajramId_n:dep_n}
   *
   * @param inputBatcherSupplier Supplies the {@link InputBatcher} corresponding to an {@link
   *     InputBatchingDecorator}. This supplier is guaranteed to be called exactly once for every
   *     unique {@link InputBatchingDecorator} instance.
   */
  public static InputBatcherConfig simple(Supplier<InputBatcher> inputBatcherSupplier) {
    return new InputBatcherConfig(
        logicExecutionContext ->
            generateInstanceId(
                    logicExecutionContext.dependants(),
                    logicExecutionContext.kryonDefinitionRegistry())
                .toString(),
        batcherContext -> true,
        batcherContext -> {
          @SuppressWarnings("unchecked")
          BatchableSupplier<Facets, Facets> facetsConvertor =
              (BatchableSupplier<Facets, Facets>) batcherContext.vajram().getBatchFacetsConvertor();
          return new InputBatchingDecorator(
              batcherContext.decoratorContext().instanceId(),
              inputBatcherSupplier.get(),
              facetsConvertor,
              dependantChain ->
                  batcherContext
                      .decoratorContext()
                      .logicExecutionContext()
                      .dependants()
                      .equals(dependantChain));
        });
  }

  public static InputBatcherConfig sharedBatcher(
      Supplier<InputBatcher> inputBatcherSupplier,
      String instanceId,
      DependantChain... dependantChains) {
    return sharedBatcher(inputBatcherSupplier, instanceId, ImmutableSet.copyOf(dependantChains));
  }

  public static InputBatcherConfig sharedBatcher(
      Supplier<InputBatcher> inputBatcherSupplier,
      String instanceId,
      ImmutableSet<DependantChain> dependantChains) {
    return new InputBatcherConfig(
        logicExecutionContext -> instanceId,
        batcherContext ->
            dependantChains.contains(
                batcherContext.decoratorContext().logicExecutionContext().dependants()),
        batcherContext -> {
          @SuppressWarnings("unchecked")
          BatchableSupplier<Facets, Facets> facetsConvertor =
              (BatchableSupplier<Facets, Facets>) batcherContext.vajram().getBatchFacetsConvertor();
          return new InputBatchingDecorator(
              instanceId, inputBatcherSupplier.get(), facetsConvertor, dependantChains::contains);
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
            AnnotationTags.getNamedValueTag(
                    VajramTags.VAJRAM_ID,
                    kryonDefinitionRegistry
                        .get(defaultDependantChain.kryonId())
                        .getOutputLogicDefinition()
                        .logicTags())
                .orElseThrow(
                    () ->
                        new NoSuchElementException(
                            "Could not find tag %s for kryon %s"
                                .formatted(VajramTags.VAJRAM_ID, defaultDependantChain.kryonId())))
                .value();
        return generateInstanceId(defaultDependantChain.dependantChain(), kryonDefinitionRegistry)
            .append('>')
            .append(vajramId)
            .append(':')
            .append(defaultDependantChain.dependencyId());
      } else {
        return generateInstanceId(defaultDependantChain.dependantChain(), kryonDefinitionRegistry)
            .append('>')
            .append(defaultDependantChain.dependencyId());
      }
    }
    throw new UnsupportedOperationException();
  }

  public record BatcherContext(BatchableVajram<?> vajram, DecoratorContext decoratorContext) {}
}
