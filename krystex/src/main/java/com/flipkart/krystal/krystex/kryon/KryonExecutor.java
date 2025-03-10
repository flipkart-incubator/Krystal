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

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.annos.TraitDependency;
import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardReceive;
import com.flipkart.krystal.krystex.commands.ForwardSend;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecoratorConfig;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyExecutionContext;
import com.flipkart.krystal.krystex.dependencydecorators.TraitDispatchDecorator;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorationInput;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorator;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorContext;
import com.flipkart.krystal.krystex.kryondecoration.KryonExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.InitiateActiveDepChains;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig.LogicDecoratorContext;
import com.flipkart.krystal.krystex.request.IntReqGenerator;
import com.flipkart.krystal.krystex.request.RequestId;
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
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Default implementation of Krystal executor which */
@Slf4j
public final class KryonExecutor implements KrystalExecutor {

  public enum KryonExecStrategy {
    BATCH,
  }

  public enum GraphTraversalStrategy {
    DEPTH,
    BREADTH,
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
  private final ImmutableMap<
          String, // DecoratorType
          List<OutputLogicDecoratorConfig>>
      requestScopedLogicDecoratorConfigs;

  private final ImmutableMap<String, DependencyDecoratorConfig> dependencyDecoratorConfigs;

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
      requestScopedKryonDecorators = new LinkedHashMap<>();

  private final Map<
          String, // DecoratorType
          Map<
              String, // InstanceId
              DependencyDecorator>>
      dependencyDecorators = new LinkedHashMap<>();

  private final KryonRegistry<?> kryonRegistry = new KryonRegistry<>();
  private final KryonExecutorMetrics kryonMetrics;
  private final Map<RequestId, KryonExecution> allExecutions = new LinkedHashMap<>();
  private final Set<RequestId> unFlushedExecutions = new LinkedHashSet<>();
  private final Map<VajramID, Set<DependantChain>> dependantChainsPerKryon = new LinkedHashMap<>();
  private final RequestIdGenerator preferredReqGenerator;
  private final Set<DependantChain> depChainsDisabledInAllExecutions = new LinkedHashSet<>();

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
    this.requestScopedLogicDecoratorConfigs =
        ImmutableMap.copyOf(executorConfig.requestScopedLogicDecoratorConfigs());
    this.dependencyDecoratorConfigs = makeDependencyDecorConfigs(executorConfig);
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
    VajramKryonDefinition kryonDefinition =
        KryonUtils.validateAsVajram(kryonDefinitionRegistry.getOrThrow(vajramID));
    OutputLogicDefinition<?> outputLogicDefinition = kryonDefinition.getOutputLogicDefinition();
    Map<String, OutputLogicDecorator> decorators = new LinkedHashMap<>();
    Stream.concat(
            outputLogicDefinition.requestScopedLogicDecoratorConfigs().entrySet().stream(),
            requestScopedLogicDecoratorConfigs.entrySet().stream())
        .forEach(
            entry -> {
              String decoratorType = entry.getKey();
              if (decorators.containsKey(decoratorType)) {
                return;
              }
              List<OutputLogicDecoratorConfig> decoratorConfigList =
                  new ArrayList<>(entry.getValue());
              for (OutputLogicDecoratorConfig decoratorConfig : decoratorConfigList) {
                if (decoratorConfig.shouldDecorate().test(logicExecutionContext)) {
                  String instanceId =
                      decoratorConfig.instanceIdGenerator().apply(logicExecutionContext);
                  OutputLogicDecorator outputLogicDecorator =
                      outputLogicDecorators
                          .computeIfAbsent(decoratorType, t -> new LinkedHashMap<>())
                          .computeIfAbsent(
                              instanceId,
                              _i ->
                                  decoratorConfig
                                      .factory()
                                      .apply(
                                          new LogicDecoratorContext(
                                              instanceId, logicExecutionContext)));
                  outputLogicDecorator.executeCommand(
                      new InitiateActiveDepChains(
                          vajramID,
                          ImmutableSet.copyOf(
                              dependantChainsPerKryon.getOrDefault(vajramID, ImmutableSet.of()))));
                  decorators.put(decoratorType, outputLogicDecorator);
                  break;
                }
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
      final VajramID vajramID, Request facets, KryonExecutionConfig executionConfig) {
    if (closed) {
      throw new RejectedExecutionException("KryonExecutor is already closed");
    }
    checkArgument(executionConfig != null, "executionConfig can not be null");
    if (!executorConfig._riskyOpenAllKryonsForExternalInvocation()) {
      if (!kryonDefinitionRegistry
          .getOrThrow(vajramID)
          .tags()
          .getAnnotationByType(ExternalInvocation.class)
          .map(ExternalInvocation::allow)
          .orElse(false)) {
        throw new RejectedExecutionException(
            "External invocation is not allowed for vajramId: " + vajramID);
      }
    }

    String executionId = executionConfig.executionId();
    checkArgument(executionId != null, "executionConfig.executionId can not be null");
    RequestId requestId =
        preferredReqGenerator.newRequest("%s:%s".formatted(instanceId, executionId));

    //noinspection RedundantCast: This is to avoid nullChecker failing compilation.
    return enqueueCommand(
            // Perform all datastructure manipulations in the command queue to avoid multi-thread
            // access
            (Supplier<CompletableFuture<@Nullable T>>)
                () -> {
                  createDependencyKryons(
                      vajramID, kryonDefinitionRegistry.getDependantChainsStart(), executionConfig);
                  CompletableFuture<@Nullable Object> future = new CompletableFuture<>();
                  if (allExecutions.containsKey(requestId)) {
                    future.completeExceptionally(
                        new IllegalArgumentException(
                            "Received duplicate requests for same instanceId '%s' and execution Id '%s'"
                                .formatted(instanceId, executionId)));
                  } else {
                    //noinspection unchecked
                    allExecutions.put(
                        requestId,
                        new KryonExecution(
                            vajramID, requestId, (Request) facets, executionConfig, future));
                    unFlushedExecutions.add(requestId);
                  }

                  @SuppressWarnings("unchecked")
                  CompletableFuture<@Nullable T> f = (CompletableFuture<@Nullable T>) future;
                  return f;
                })
        .thenCompose(identity());
  }

  private void createDependencyKryons(
      VajramID vajramID, DependantChain dependantChain, KryonExecutionConfig executionConfig) {
    if (union(executorConfig.disabledDependantChains(), executionConfig.disabledDependantChains())
        .contains(dependantChain)) {
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
        Dependency latestDependency = dependantChain.latestDependency();
        VajramID boundVajram;
        try {
          if (latestDependency != null) {
            boundVajram = staticDispatchPolicy.get(latestDependency);
          } else {
            boundVajram = staticDispatchPolicy.get(executionConfig.staticDispatchQualifer());
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
                  depKryonId, dependantChain.extend(finalVajramId, dependency), executionConfig));
      dependantChainsPerKryon
          .computeIfAbsent(finalVajramId, _n -> new LinkedHashSet<>())
          .add(dependantChain);
    }
  }

  @SuppressWarnings("unchecked")
  private Kryon<? extends KryonCommand, ? extends KryonResponse> createKryonIfAbsent(
      VajramID vajramID, VajramKryonDefinition kryonDefinition) {
    KryonRegistry<BatchKryon> batchKryonRegistry = (KryonRegistry<BatchKryon>) kryonRegistry;
    return batchKryonRegistry.createIfAbsent(
        vajramID,
        _n ->
            new BatchKryon(
                kryonDefinition,
                this,
                this::getOutputLogicDecorators,
                this::getDependencyDecorators,
                executorConfig.logicDecorationOrdering(),
                preferredReqGenerator));
  }

  /**
   * Enqueues the provided KryonCommand supplier into the command queue. This method is intended to
   * be called in threads other than the main thread of this KryonExecutor.(for example IO reactor
   * threads). When a non-blocking IO call is made by a kryon, a callback is added to the resulting
   * CompletableFuture which generates an ExecuteWithDependency command for its dependents. That is
   * when this method is used - ensuring that all further processing of the kryonCammand happens in
   * the main thread.
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  <R extends KryonResponse> CompletableFuture<R> enqueueKryonCommand(
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
   * pontentially unnecessary contention in the thread-safe structures inside the command queue.
   */
  public <R extends KryonResponse> CompletableFuture<R> executeCommand(
      KryonCommand<R> kryonCommand) {
    if (BREADTH.equals(executorConfig.graphTraversalStrategy())) {
      return enqueueKryonCommand(() -> kryonCommand);
    } else {
      kryonMetrics.commandQueueBypassed();
      return _executeCommand(kryonCommand);
    }
  }

  private <R extends KryonResponse> CompletableFuture<R> _executeCommand(
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
                forwardSend.dependantChain(),
                forwardSend.skippedRequests()));
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
                    new KryonDecorationInput((Kryon<KryonCommand, KryonResponse>) kryon, this));
        kryon = decoratedKryon;
      }
      if (kryonCommand instanceof Flush flush) {
        kryon.executeCommand(flush);
        @SuppressWarnings("unchecked")
        CompletableFuture<R> f = completedFuture((R) FlushResponse.getInstance());
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
    Map<String, KryonDecoratorConfig> configs = executorConfig.kryonDecoratorConfigs();
    KryonExecutionContext executionContext =
        new KryonExecutionContext(vajramID, kryonCommand.dependantChain());
    TreeSet<KryonDecorator> sortedDecorators =
        new TreeSet<>(executorConfig.logicDecorationOrdering().encounterOrder().reversed());
    for (Entry<String, KryonDecoratorConfig> configsByType : configs.entrySet()) {
      String decoratorType = configsByType.getKey();
      KryonDecoratorConfig decoratorConfig = configsByType.getValue();
      if (!decoratorConfig.shouldDecorate().test(executionContext)) {
        continue;
      }
      String instanceId = decoratorConfig.instanceIdGenerator().apply(executionContext);
      sortedDecorators.add(
          requestScopedKryonDecorators
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
    DependantChain dependantChain = kryonCommand.dependantChain();
    if (depChainsDisabledInAllExecutions.contains(dependantChain)) {
      throw new DisabledDependantChainException(dependantChain);
    }
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void flush() {
    enqueueRunnable(
        () -> {
          computeDisabledDependantChains();
          submitBatch(unFlushedExecutions);
          unFlushedExecutions.stream()
              .map(requestId -> getKryonExecution(requestId).vajramID())
              .distinct()
              .forEach(
                  kryonId -> {
                    executorConfig
                        .traitDispatchDecorator()
                        .invokeDependency(
                            new Flush(kryonId, kryonDefinitionRegistry.getDependantChainsStart()),
                            this::executeCommand);
                  });
        });
  }

  private void computeDisabledDependantChains() {
    depChainsDisabledInAllExecutions.clear();
    List<ImmutableSet<DependantChain>> disabledDependantChainsPerExecution =
        unFlushedExecutions.stream()
            .map(this::getKryonExecution)
            .map(KryonExecution::executionConfig)
            .map(KryonExecutionConfig::disabledDependantChains)
            .toList();
    disabledDependantChainsPerExecution.stream()
        .filter(x -> !x.isEmpty())
        .findAny()
        .ifPresent(depChainsDisabledInAllExecutions::addAll);
    for (Set<DependantChain> disabledDepChains : disabledDependantChainsPerExecution) {
      if (depChainsDisabledInAllExecutions.isEmpty()) {
        break;
      }
      depChainsDisabledInAllExecutions.retainAll(disabledDepChains);
    }
    depChainsDisabledInAllExecutions.addAll(executorConfig.disabledDependantChains());
  }

  private KryonExecution getKryonExecution(RequestId requestId) {
    KryonExecution kryonExecution = allExecutions.get(requestId);
    if (kryonExecution == null) {
      throw new AssertionError("No kryon execution found for requestId " + requestId);
    }
    return kryonExecution;
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void submitBatch(Set<RequestId> unFlushedRequests) {
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
                    .invokeDependency(
                        new ForwardSend(
                            kryonId,
                            kryonExecutions.stream()
                                .collect(
                                    toImmutableMap(
                                        KryonExecution::instanceExecutionId,
                                        KryonExecution::request)),
                            kryonDefinitionRegistry.getDependantChainsStart(),
                            ImmutableMap.of()),
                        this::executeCommand);
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
                        Errable<Object> result =
                            responses.getOrDefault(
                                kryonExecution.instanceExecutionId(), Errable.nil());
                        linkFutures(result.toFuture(), kryonExecution.future());
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
      RequestId instanceExecutionId,
      Request request,
      KryonExecutionConfig executionConfig,
      CompletableFuture<@Nullable Object> future) {}
}
