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

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetsBuilder;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardReceive;
import com.flipkart.krystal.krystex.commands.ForwardSend;
import com.flipkart.krystal.krystex.commands.KryonCommand;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
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
  private final ExecutorService commandQueue;
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

  private final Map<
          String, // DecoratorType
          Map<
              String, // InstanceId
              OutputLogicDecorator>>
      requestScopedOutputLogicDecorators = new LinkedHashMap<>();

  private final Map<
          String, // DecoratorType
          Map<
              String, // InstanceId
              KryonDecorator>>
      requestScopedKryonDecorators = new LinkedHashMap<>();

  private final KryonRegistry<?> kryonRegistry = new KryonRegistry<>();
  private final KryonExecutorMetrics kryonMetrics;
  private final Map<RequestId, KryonExecution> allExecutions = new LinkedHashMap<>();
  private final Set<RequestId> unFlushedExecutions = new LinkedHashSet<>();
  private final Map<KryonId, Set<DependantChain>> dependantChainsPerKryon = new LinkedHashMap<>();
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
    this.kryonMetrics = new KryonExecutorMetrics();
    this.preferredReqGenerator =
        executorConfig.debug() ? new StringReqGenerator() : new IntReqGenerator();
  }

  private ImmutableMap<String, OutputLogicDecorator> getRequestScopedDecorators(
      LogicExecutionContext logicExecutionContext) {
    KryonId kryonId = logicExecutionContext.kryonId();
    KryonDefinition kryonDefinition = kryonDefinitionRegistry.get(kryonId);
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
                String instanceId =
                    decoratorConfig.instanceIdGenerator().apply(logicExecutionContext);
                if (decoratorConfig.shouldDecorate().test(logicExecutionContext)) {
                  OutputLogicDecorator outputLogicDecorator =
                      requestScopedOutputLogicDecorators
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
                          kryonId,
                          ImmutableSet.copyOf(
                              dependantChainsPerKryon.getOrDefault(kryonId, ImmutableSet.of()))));
                  decorators.put(decoratorType, outputLogicDecorator);
                  break;
                }
              }
            });
    return ImmutableMap.copyOf(decorators);
  }

  @Override
  public <T> CompletableFuture<@Nullable T> executeKryon(
      KryonId kryonId, Request facets, KryonExecutionConfig executionConfig) {
    if (closed) {
      throw new RejectedExecutionException("KryonExecutor is already closed");
    }
    checkArgument(executionConfig != null, "executionConfig can not be null");
    if (!kryonDefinitionRegistry
        .get(kryonId)
        .tags()
        .getAnnotationByType(ExternalInvocation.class)
        .map(ExternalInvocation::allow)
        .orElse(false)) {
      throw new RejectedExecutionException(
          "External invocation is not allowed for kryonId: " + kryonId);
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
                (() -> {
                  createDependencyKryons(
                      kryonId, kryonDefinitionRegistry.getDependantChainsStart(), executionConfig);
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
                            kryonId, requestId, (Request) facets, executionConfig, future));
                    unFlushedExecutions.add(requestId);
                  }

                  @SuppressWarnings("unchecked")
                  CompletableFuture<@Nullable T> f = (CompletableFuture<@Nullable T>) future;
                  return f;
                }))
        .thenCompose(identity());
  }

  private void createDependencyKryons(
      KryonId kryonId, DependantChain dependantChain, KryonExecutionConfig executionConfig) {
    KryonDefinition kryonDefinition = kryonDefinitionRegistry.get(kryonId);
    // If a dependantChain is disabled, don't create that kryon and its dependency kryons
    if (!union(executorConfig.disabledDependantChains(), executionConfig.disabledDependantChains())
        .contains(dependantChain)) {
      createKryonIfAbsent(kryonId, kryonDefinition);
      ImmutableMap<Dependency, KryonId> dependencyKryons = kryonDefinition.dependencyKryons();
      dependencyKryons.forEach(
          (dependency, depKryonId) ->
              createDependencyKryons(
                  depKryonId, dependantChain.extend(kryonId, dependency), executionConfig));
      dependantChainsPerKryon
          .computeIfAbsent(kryonId, _n -> new LinkedHashSet<>())
          .add(dependantChain);
    }
  }

  @SuppressWarnings("unchecked")
  private void createKryonIfAbsent(KryonId kryonId, KryonDefinition kryonDefinition) {
    KryonRegistry<BatchKryon> batchKryonRegistry = (KryonRegistry<BatchKryon>) kryonRegistry;
    batchKryonRegistry.createIfAbsent(
        kryonId,
        _n ->
            new BatchKryon(
                kryonDefinition,
                this,
                this::getRequestScopedDecorators,
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
  public <T extends KryonResponse> CompletableFuture<T> executeCommand(KryonCommand kryonCommand) {
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
        return _executeCommand(
            new ForwardReceive(
                forwardSend.kryonId(),
                forwardSend.executableRequests().entrySet().stream()
                    .collect(
                        toImmutableMap(
                            Entry::getKey,
                            e ->
                                kryonDefinitionRegistry
                                    .get(kryonCommand.kryonId())
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
      KryonId kryonId = kryonCommand.kryonId();
      @SuppressWarnings("unchecked")
      Kryon<KryonCommand, R> kryon = (Kryon<KryonCommand, R>) kryonRegistry.get(kryonId);
      for (KryonDecorator kryonDecorator : getSortedKryonDecorators(kryonId, kryonCommand)) {
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

  private TreeSet<KryonDecorator> getSortedKryonDecorators(
      KryonId kryonId, KryonCommand kryonCommand) {
    Map<String, KryonDecoratorConfig> configs = executorConfig.requestScopedKryonDecoratorConfigs();
    KryonExecutionContext executionContext =
        new KryonExecutionContext(kryonId, kryonCommand.dependantChain());
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

  private void flush() {
    enqueueRunnable(
        () -> {
          computeDisabledDependantChains();
          submitBatch(unFlushedExecutions);
          unFlushedExecutions.stream()
              .map(requestId -> getKryonExecution(requestId).kryonId())
              .distinct()
              .forEach(
                  kryonId ->
                      executeCommand(
                          new Flush(kryonId, kryonDefinitionRegistry.getDependantChainsStart())));
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

  private void submitBatch(Set<RequestId> unFlushedRequests) {
    unFlushedRequests.stream()
        .map(this::getKryonExecution)
        .collect(groupingBy(KryonExecution::kryonId))
        .forEach(
            (kryonId, kryonResults) -> {
              CompletableFuture<BatchResponse> batchResponseFuture =
                  this.executeCommand(
                      new ForwardReceive(
                          kryonId,
                          kryonResults.stream()
                              .collect(
                                  toImmutableMap(
                                      KryonExecution::instanceExecutionId,
                                      kryonExecution1 ->
                                          kryonDefinitionRegistry
                                              .get(kryonId)
                                              .facetsFromRequest()
                                              .logic()
                                              .facetsFromRequest(kryonExecution1.request()))),
                          kryonDefinitionRegistry.getDependantChainsStart(),
                          ImmutableMap.of()));
              batchResponseFuture
                  .thenApply(BatchResponse::responses)
                  .whenComplete(
                      (responses, throwable) -> {
                        for (KryonExecution kryonExecution : kryonResults) {
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
                  allOf(kryonResults.stream().map(getFuture()).toArray(CompletableFuture[]::new)),
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
                          requestScopedOutputLogicDecorators.entrySet()) {
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
      KryonId kryonId,
      RequestId instanceExecutionId,
      Request request,
      KryonExecutionConfig executionConfig,
      CompletableFuture<@Nullable Object> future) {}
}
