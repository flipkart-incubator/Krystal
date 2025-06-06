package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.concurrent.Futures.linkFutures;
import static com.flipkart.krystal.concurrent.Futures.propagateCancellation;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.BREADTH;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Sets.union;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static lombok.AccessLevel.PACKAGE;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.annos.TraitDependency;
import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardReceive;
import com.flipkart.krystal.krystex.commands.ForwardSend;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.commands.VoidResponse;
import com.flipkart.krystal.krystex.decoration.InitiateActiveDepChains;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecoratorConfig;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyExecutionContext;
import com.flipkart.krystal.krystex.dependencydecorators.TraitDispatchDecorator;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorationInput;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorator;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorContext;
import com.flipkart.krystal.krystex.kryondecoration.KryonExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig.OutputLogicDecoratorContext;
import com.flipkart.krystal.krystex.request.IntReqGenerator;
import com.flipkart.krystal.krystex.request.InvocationId;
import com.flipkart.krystal.krystex.request.RequestIdGenerator;
import com.flipkart.krystal.krystex.request.StringReqGenerator;
import com.flipkart.krystal.traits.PredicateDynamicDispatchPolicy;
import com.flipkart.krystal.traits.StaticDispatchPolicy;
import com.flipkart.krystal.traits.TraitDispatchPolicy;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Default implementation of Krystal executor which */
@Slf4j
public final class KryonExecutor implements KrystalExecutor {

  public enum KryonExecStrategy {
    BATCH;
  }

  public enum GraphTraversalStrategy {
    DEPTH,
    BREADTH;
  }

  private final KryonDefinitionRegistry kryonDefinitionRegistry;
  private final KryonExecutorConfig executorConfig;

  @Getter(PACKAGE)
  private final SingleThreadExecutor commandQueue;

  private final String instanceId;

  /**
   * We need to have a list of request scope global decorators corresponding to each type, in case
   * we want to have a decorator of one type but based on some config in request, we want to choose
   * one. Ex : Logger, based on prod or preprod env if we want to choose different types of loggers
   * Error logger or info logger
   */
  private final ImmutableMap<String, OutputLogicDecoratorConfig> outputLogicDecoratorConfigs;

  private final ImmutableMap<String, DependencyDecoratorConfig> dependencyDecoratorConfigs;
  private final ImmutableMap<String, KryonDecoratorConfig> kryonDecoratorConfigs;

  private final Map<
          String, // DecoratorType
          Map<
              String, // InstanceId
              OutputLogicDecorator>>
      outputLogicDecorators = new LinkedHashMap<>();

  private final Map<
          String, // DecoratorType
          Map<
              String, // InstanceId
              KryonDecorator>>
      kryonDecorators = new LinkedHashMap<>();

  private final Map<
          String, // DecoratorType
          Map<
              String, // InstanceId
              DependencyDecorator>>
      dependencyDecorators = new LinkedHashMap<>();

  private final KryonRegistry<?> kryonRegistry = new KryonRegistry<>();
  private final KryonExecutorMetrics kryonMetrics;
  private final Map<InvocationId, KryonExecution> allExecutions = new LinkedHashMap<>();
  private final Set<InvocationId> unFlushedExecutions = new LinkedHashSet<>();
  private final Map<VajramID, Set<DependentChain>> dependentChainsPerKryon = new LinkedHashMap<>();
  private final RequestIdGenerator preferredReqGenerator;
  private final Set<DependentChain> depChainsDisabledInAllExecutions = new LinkedHashSet<>();

  private volatile boolean closed;
  private boolean shutdownRequested;

  public KryonExecutor(
      KryonDefinitionRegistry kryonDefinitionRegistry,
      KryonExecutorConfig executorConfig,
      String instanceId) {
    this.kryonDefinitionRegistry = kryonDefinitionRegistry;
    this.executorConfig = executorConfig;
    this.commandQueue = executorConfig.singleThreadExecutor();
    this.instanceId = instanceId;
    this.outputLogicDecoratorConfigs = executorConfig.outputLogicDecoratorConfigs();
    this.dependencyDecoratorConfigs = makeDependencyDecorConfigs(executorConfig);
    this.kryonDecoratorConfigs = executorConfig.kryonDecoratorConfigs();
    this.kryonMetrics = new KryonExecutorMetrics();
    this.preferredReqGenerator =
        executorConfig.debug() ? new StringReqGenerator() : new IntReqGenerator();
  }

  private static ImmutableMap<String, DependencyDecoratorConfig> makeDependencyDecorConfigs(
      KryonExecutorConfig executorConfig) {
    var builder =
        ImmutableMap.<String, DependencyDecoratorConfig>builder()
            .putAll(executorConfig.dependencyDecoratorConfigs());
    TraitDispatchDecorator traitDispatchDecorator = executorConfig.traitDispatchDecorator();
    if (traitDispatchDecorator != null) {
      String decoratorType = traitDispatchDecorator.decoratorType();
      builder.put(
          decoratorType,
          new DependencyDecoratorConfig(
              decoratorType,
              dependencyExecutionContext ->
                  dependencyExecutionContext
                      .dependency()
                      .tags()
                      .getAnnotationByType(TraitDependency.class)
                      .isPresent(),
              d -> decoratorType,
              c -> traitDispatchDecorator));
    }

    return builder.build();
  }

  private ImmutableMap<String, OutputLogicDecorator> getOutputLogicDecorators(
      LogicExecutionContext logicExecutionContext) {
    VajramID vajramID = logicExecutionContext.vajramID();
    Map<String, OutputLogicDecorator> decorators = new LinkedHashMap<>();
    outputLogicDecoratorConfigs.forEach(
        (decoratorType, decoratorConfig) -> {
          if (decoratorConfig.shouldDecorate().test(logicExecutionContext)) {
            String instanceId = decoratorConfig.instanceIdGenerator().apply(logicExecutionContext);
            OutputLogicDecorator outputLogicDecorator =
                outputLogicDecorators
                    .computeIfAbsent(decoratorType, t -> new LinkedHashMap<>())
                    .computeIfAbsent(
                        instanceId,
                        _i -> {
                          OutputLogicDecorator logicDecorator =
                              decoratorConfig
                                  .factory()
                                  .apply(
                                      new OutputLogicDecoratorContext(
                                          instanceId, logicExecutionContext));
                          logicDecorator.executeCommand(
                              new InitiateActiveDepChains(
                                  vajramID,
                                  ImmutableSet.copyOf(
                                      dependentChainsPerKryon.getOrDefault(
                                          vajramID, ImmutableSet.of()))));
                          return logicDecorator;
                        });
            decorators.put(decoratorType, outputLogicDecorator);
          }
        });
    return ImmutableMap.copyOf(decorators);
  }

  private ImmutableMap<String, DependencyDecorator> getDependencyDecorators(
      DependencyExecutionContext dependencyExecutionContext) {
    Map<String, DependencyDecorator> decorators = new LinkedHashMap<>();
    for (Entry<String, DependencyDecoratorConfig> entry : dependencyDecoratorConfigs.entrySet()) {
      String decoratorType = entry.getKey();
      DependencyDecoratorConfig decoratorConfig = entry.getValue();
      if (decoratorConfig.shouldDecorate().test(dependencyExecutionContext)) {
        String instanceId = decoratorConfig.instanceIdGenerator().apply(dependencyExecutionContext);
        DependencyDecorator dependencyDecorator =
            dependencyDecorators
                .computeIfAbsent(decoratorType, s -> new LinkedHashMap<>())
                .computeIfAbsent(
                    instanceId, s -> decoratorConfig.factory().apply(dependencyExecutionContext));
        decorators.put(decoratorType, dependencyDecorator);
      }
    }
    return ImmutableMap.copyOf(decorators);
  }

  @Override
  @SuppressWarnings("FutureReturnValueIgnored")
  public <T> CompletableFuture<@Nullable T> executeKryon(
      ImmutableRequest request, KryonExecutionConfig executionConfig) {
    if (closed) {
      throw new RejectedExecutionException("KryonExecutor is already closed");
    }
    checkArgument(executionConfig != null, "executionConfig can not be null");
    VajramID vajramID = request._vajramID();
    @SuppressWarnings("deprecation")
    boolean openAllKryonsForExternalInvocation =
        executorConfig._riskyOpenAllKryonsForExternalInvocation();
    if (!openAllKryonsForExternalInvocation) {
      if (kryonDefinitionRegistry
          .getOrThrow(vajramID)
          .tags()
          .getAnnotationByType(ExternallyInvocable.class)
          .isEmpty()) {
        throw new RejectedExecutionException(
            "External invocation is not allowed for vajramId: " + vajramID);
      }
    }

    String executionId = executionConfig.executionId();
    checkArgument(executionId != null, "executionConfig.executionId can not be null");
    InvocationId invocationId =
        preferredReqGenerator.newRequest("%s:%s".formatted(instanceId, executionId));

    //noinspection RedundantCast: This is to avoid nullChecker failing compilation.
    return enqueueCommand(
            // Perform all data-structure manipulations in the command queue to avoid multi-thread
            // access
            (Supplier<CompletableFuture<@Nullable T>>)
                () -> {
                  createDependencyKryons(
                      vajramID, kryonDefinitionRegistry.getDependentChainsStart(), executionConfig);
                  CompletableFuture<@Nullable Object> future = new CompletableFuture<>();
                  if (allExecutions.containsKey(invocationId)) {
                    future.completeExceptionally(
                        new IllegalArgumentException(
                            "Received duplicate requests for same instanceId '%s' and execution Id '%s'"
                                .formatted(instanceId, executionId)));
                  } else {
                    //noinspection unchecked
                    allExecutions.put(
                        invocationId,
                        new KryonExecution(
                            vajramID, invocationId, request, executionConfig, future));
                    unFlushedExecutions.add(invocationId);
                  }

                  @SuppressWarnings("unchecked")
                  CompletableFuture<@Nullable T> f = (CompletableFuture<@Nullable T>) future;
                  return f;
                })
        .thenCompose(identity());
  }

  private void createDependencyKryons(
      VajramID vajramID, DependentChain dependentChain, KryonExecutionConfig executionConfig) {
    if (union(executorConfig.disabledDependentChains(), executionConfig.disabledDependentChains())
        .contains(dependentChain)) {
      // If a dependantChain is disabled, don't create that kryon and its dependency kryons
      return;
    }
    KryonDefinition kryonDefinition = kryonDefinitionRegistry.get(vajramID);
    List<VajramID> concreteVajramIds = new ArrayList<>();
    if (kryonDefinition instanceof TraitKryonDefinition) {
      @Nullable TraitDispatchPolicy traitDispatchPolicy =
          executorConfig.traitDispatchDecorator().traitDispatchPolicies().get(vajramID);
      if (traitDispatchPolicy == null) {
        throw new IllegalArgumentException(
            "Trait "
                + vajramID
                + " found but no TraitDispatchPolicy provided in the executorConfig");
      } else if (traitDispatchPolicy instanceof StaticDispatchPolicy staticDispatchPolicy) {
        Dependency latestDependency = dependentChain.latestDependency();
        VajramID boundVajram;
        try {
          if (latestDependency != null) {
            boundVajram = staticDispatchPolicy.get(latestDependency);
          } else {
            boundVajram = staticDispatchPolicy.get(executionConfig.staticDispatchQualifier());
          }
        } catch (Throwable throwable) {
          throw new IllegalArgumentException(
              "Error while getting bound vajram for trait with ID: " + vajramID);
        }
        concreteVajramIds.add(boundVajram);

      } else if (traitDispatchPolicy instanceof PredicateDynamicDispatchPolicy dynamicDispatcher) {
        concreteVajramIds.addAll(dynamicDispatcher.dispatchTargets());
      }
    } else {
      concreteVajramIds.add(vajramID);
    }
    for (VajramID finalVajramId : concreteVajramIds) {
      ImmutableMap<Dependency, VajramID> dependencyKryons = ImmutableMap.of();
      kryonDefinition = kryonDefinitionRegistry.getOrThrow(finalVajramId);
      if (kryonDefinition instanceof VajramKryonDefinition vajramKryonDefinition) {
        createKryonIfAbsent(finalVajramId, vajramKryonDefinition);
        dependencyKryons = vajramKryonDefinition.dependencyKryons();
      }
      dependencyKryons.forEach(
          (dependency, depKryonId) ->
              createDependencyKryons(
                  depKryonId, dependentChain.extend(finalVajramId, dependency), executionConfig));
      dependentChainsPerKryon
          .computeIfAbsent(finalVajramId, _n -> new LinkedHashSet<>())
          .add(dependentChain);
    }
  }

  @SuppressWarnings("unchecked")
  private Kryon<? extends KryonCommand, ? extends KryonCommandResponse> createKryonIfAbsent(
      VajramID vajramID, VajramKryonDefinition kryonDefinition) {
    KryonRegistry<FlushableKryon> batchKryonRegistry =
        (KryonRegistry<FlushableKryon>) kryonRegistry;
    return batchKryonRegistry.createIfAbsent(
        vajramID,
        _n ->
            new FlushableKryon(
                kryonDefinition,
                this,
                this::getOutputLogicDecorators,
                this::getDependencyDecorators,
                executorConfig.decorationOrdering(),
                preferredReqGenerator));
  }

  /**
   * Enqueues the provided KryonCommand supplier into the command queue. This method is intended to
   * be called in threads other than the main thread of this KryonExecutor.(for example IO reactor
   * threads). When a non-blocking IO call is made by a kryon, a callback is added to the resulting
   * CompletableFuture which generates an ExecuteWithDependency command for its dependents. That is
   * when this method is used - ensuring that all further processing of the kryonCommand happens in
   * the main thread.
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  <R extends KryonCommandResponse> CompletableFuture<R> enqueueKryonCommand(
      Supplier<? extends KryonCommand> kryonCommand) {
    return enqueueCommand(
            (Supplier<CompletableFuture<R>>) () -> _executeCommand(kryonCommand.get()))
        .thenCompose(identity());
  }

  /**
   * When using {@link GraphTraversalStrategy#DEPTH}, this method can be called only from the main
   * thread of this KryonExecutor. Calling this method from any other thread (for example: IO
   * reactor threads) will cause race conditions, multithreaded access of non-thread-safe data
   * structures, and resulting unspecified behaviour.
   *
   * <p>When using {@link GraphTraversalStrategy#DEPTH}, this is a more optimal version of {@link
   * #enqueueKryonCommand(Supplier)} as it bypasses the command queue for the special case that the
   * command is originating from the same main thread inside the command queue,thus avoiding the
   * potentially unnecessary contention in the thread-safe structures inside the command queue.
   */
  public <R extends KryonCommandResponse> CompletableFuture<R> executeCommand(
      KryonCommand<R> kryonCommand) {
    if (BREADTH.equals(executorConfig.graphTraversalStrategy())) {
      return enqueueKryonCommand(() -> kryonCommand);
    } else {
      kryonMetrics.commandQueueBypassed();
      return _executeCommand(kryonCommand);
    }
  }

  private <R extends KryonCommandResponse> CompletableFuture<R> _executeCommand(
      KryonCommand kryonCommand) {
    try {
      if (kryonCommand instanceof ForwardSend forwardSend) {
        VajramKryonDefinition vajramKryonDefinition =
            KryonUtils.validateAsVajram(
                kryonDefinitionRegistry.getOrThrow(kryonCommand.vajramID()));
        return _executeCommand(
            new ForwardReceive(
                forwardSend.vajramID(),
                forwardSend.executableRequests().entrySet().stream()
                    .collect(
                        toImmutableMap(
                            Entry::getKey,
                            e ->
                                vajramKryonDefinition
                                    .facetsFromRequest()
                                    .logic()
                                    .facetsFromRequest(e.getValue()))),
                forwardSend.dependentChain(),
                forwardSend.skippedInvocations()));
      }
      try {
        validate(kryonCommand);
      } catch (Throwable e) {
        return failedFuture(e);
      }
      VajramID vajramID = kryonCommand.vajramID();
      VajramKryonDefinition kryonDefinition =
          KryonUtils.validateAsVajram(kryonDefinitionRegistry.getOrThrow(vajramID));
      @SuppressWarnings("unchecked")
      Kryon<KryonCommand, R> kryon =
          (Kryon<KryonCommand, R>) createKryonIfAbsent(vajramID, kryonDefinition);
      for (KryonDecorator kryonDecorator : getSortedKryonDecorators(vajramID, kryonCommand)) {
        @SuppressWarnings("unchecked")
        Kryon<KryonCommand, R> decoratedKryon =
            (Kryon<KryonCommand, R>)
                kryonDecorator.decorateKryon(
                    new KryonDecorationInput(
                        (Kryon<KryonCommand, KryonCommandResponse>) kryon, this));
        kryon = decoratedKryon;
      }
      if (kryonCommand instanceof Flush flush) {
        kryon.executeCommand(flush);
        @SuppressWarnings("unchecked")
        CompletableFuture<R> f = completedFuture((R) VoidResponse.getInstance());
        return f;
      } else {
        return kryon.executeCommand(kryonCommand);
      }
    } catch (Throwable e) {
      return failedFuture(e);
    }
  }

  private Set<KryonDecorator> getSortedKryonDecorators(
      VajramID vajramID, KryonCommand kryonCommand) {
    KryonExecutionContext executionContext =
        new KryonExecutionContext(vajramID, kryonCommand.dependentChain());
    TreeSet<KryonDecorator> sortedDecorators =
        new TreeSet<>(executorConfig.decorationOrdering().encounterOrder().reversed());
    for (Entry<String, KryonDecoratorConfig> configsByType : kryonDecoratorConfigs.entrySet()) {
      String decoratorType = configsByType.getKey();
      KryonDecoratorConfig decoratorConfig = configsByType.getValue();
      if (!decoratorConfig.shouldDecorate().test(executionContext)) {
        continue;
      }
      String instanceId = decoratorConfig.instanceIdGenerator().apply(executionContext);
      sortedDecorators.add(
          kryonDecorators
              .computeIfAbsent(decoratorType, _t -> new LinkedHashMap<>())
              .computeIfAbsent(
                  instanceId,
                  _i ->
                      decoratorConfig
                          .factory()
                          .apply(new KryonDecoratorContext(instanceId, executionContext))));
    }
    return sortedDecorators;
  }

  private void validate(KryonCommand kryonCommand) {
    if (shutdownRequested) {
      throw new RejectedExecutionException("Kryon Executor shutdown requested.");
    }
    DependentChain dependentChain = kryonCommand.dependentChain();
    if (depChainsDisabledInAllExecutions.contains(dependentChain)) {
      throw new DisabledDependentChainException(dependentChain);
    }
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void flush() {
    enqueueRunnable(
        () -> {
          computeDisabledDependentChains();
          submitBatch(unFlushedExecutions);
          unFlushedExecutions.stream()
              .map(requestId -> getKryonExecution(requestId).vajramID())
              .distinct()
              .forEach(
                  kryonId ->
                      executorConfig
                          .traitDispatchDecorator()
                          .<VoidResponse>decorateDependency(this::executeCommand)
                          .invokeDependency(
                              new Flush(
                                  kryonId, kryonDefinitionRegistry.getDependentChainsStart())));
        });
  }

  private void computeDisabledDependentChains() {
    depChainsDisabledInAllExecutions.clear();
    List<ImmutableSet<DependentChain>> disabledDependantChainsPerExecution =
        unFlushedExecutions.stream()
            .map(this::getKryonExecution)
            .map(KryonExecution::executionConfig)
            .map(KryonExecutionConfig::disabledDependentChains)
            .toList();
    disabledDependantChainsPerExecution.stream()
        .filter(x -> !x.isEmpty())
        .findAny()
        .ifPresent(depChainsDisabledInAllExecutions::addAll);
    for (Set<DependentChain> disabledDepChains : disabledDependantChainsPerExecution) {
      if (depChainsDisabledInAllExecutions.isEmpty()) {
        break;
      }
      depChainsDisabledInAllExecutions.retainAll(disabledDepChains);
    }
    depChainsDisabledInAllExecutions.addAll(executorConfig.disabledDependentChains());
  }

  private KryonExecution getKryonExecution(InvocationId invocationId) {
    KryonExecution kryonExecution = allExecutions.get(invocationId);
    if (kryonExecution == null) {
      throw new AssertionError("No kryon execution found for requestId " + invocationId);
    }
    return kryonExecution;
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void submitBatch(Set<InvocationId> unFlushedRequests) {
    Map<VajramID, List<KryonExecution>> executionsByKryon =
        unFlushedRequests.stream()
            .map(this::getKryonExecution)
            .collect(groupingBy(KryonExecution::vajramID));
    executionsByKryon.forEach(
        (kryonId, kryonExecutions) -> {
          CompletableFuture<BatchResponse> batchResponseFuture;
          try {
            batchResponseFuture =
                executorConfig
                    .traitDispatchDecorator()
                    .<BatchResponse>decorateDependency(this::executeCommand)
                    .invokeDependency(
                        new ForwardSend(
                            kryonId,
                            kryonExecutions.stream()
                                .collect(
                                    ImmutableMap
                                        .<KryonExecution, InvocationId, Request<?>>toImmutableMap(
                                            KryonExecution::instanceExecutionId,
                                            KryonExecution::request)),
                            kryonDefinitionRegistry.getDependentChainsStart(),
                            ImmutableMap.of()));
          } catch (Throwable throwable) {
            batchResponseFuture =
                completedFuture(
                    new BatchResponse(
                        kryonExecutions.stream()
                            .collect(
                                toImmutableMap(
                                    KryonExecution::instanceExecutionId,
                                    _k -> Errable.withError(throwable)))));
          }
          batchResponseFuture
              .thenApply(BatchResponse::responses)
              .whenComplete(
                  (responses, throwable) -> {
                    for (KryonExecution kryonExecution : kryonExecutions) {
                      if (throwable != null) {
                        kryonExecution.future().completeExceptionally(throwable);
                      } else {
                        linkFutures(
                            responses
                                .getOrDefault(kryonExecution.instanceExecutionId(), Errable.nil())
                                .toFuture(),
                            kryonExecution.future());
                      }
                    }
                  });
          propagateCancellation(
              allOf(kryonExecutions.stream().map(getFuture()).toArray(CompletableFuture[]::new)),
              batchResponseFuture);
        });
  }

  public KryonExecutorMetrics getKryonMetrics() {
    return kryonMetrics;
  }

  /**
   * Prevents accepting new requests. For reasons of performance optimization, submitted requests
   * are executed in this method.
   */
  @Override
  @SuppressWarnings("FutureReturnValueIgnored")
  public void close() {
    if (closed) {
      return;
    }
    _close0();
    flush();
    enqueueCommand(
        () ->
            allOf(
                    allExecutions.values().stream()
                        .map(getFuture())
                        .toArray(CompletableFuture[]::new))
                .whenComplete(
                    (unused, throwable) -> {
                      for (Entry<String, Map<String, OutputLogicDecorator>> decoratorsDetails :
                          outputLogicDecorators.entrySet()) {
                        Map<String, OutputLogicDecorator> decoratorsDetailsValue =
                            decoratorsDetails.getValue();
                        for (Entry<String, OutputLogicDecorator> decorator :
                            decoratorsDetailsValue.entrySet()) {
                          decorator.getValue().onComplete();
                        }
                      }
                    }));
  }

  @Override
  public void shutdownNow() {
    _close0();
    this.shutdownRequested = true;
  }

  private void _close0() {
    this.closed = true;
  }

  private static Function<KryonExecution, CompletableFuture<@Nullable Object>> getFuture() {
    return KryonExecution::future;
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void enqueueRunnable(Runnable command) {
    enqueueCommand(
        () -> {
          command.run();
          return new Object();
        });
  }

  private <T> CompletableFuture<T> enqueueCommand(Supplier<T> command) {
    return supplyAsync(
        () -> {
          kryonMetrics.commandQueued();
          return command.get();
        },
        commandQueue);
  }

  private record KryonExecution(
      VajramID vajramID,
      InvocationId instanceExecutionId,
      ImmutableRequest request,
      KryonExecutionConfig executionConfig,
      CompletableFuture<@Nullable Object> future) {}
}
