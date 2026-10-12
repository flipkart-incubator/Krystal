package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.concurrent.Futures.linkFutures;
import static com.flipkart.krystal.concurrent.Futures.propagateCancellation;
import static com.flipkart.krystal.config.PropertyNames.RISKY_OPEN_ALL_VAJRAMS_TO_EXTERNAL_INVOCATION_PROP_NAME;
import static com.flipkart.krystal.data.RequestResponseFuture.forRequest;
import static com.flipkart.krystal.except.KrystalCompletionException.wrapAsCompletionException;
import static com.flipkart.krystal.except.KrystalExceptions.setStackTracingStrategyForCurrentThread;
import static com.flipkart.krystal.except.StackTracingStrategy.FILL;
import static com.flipkart.krystal.krystex.kryon.KryonUtils.validateAsVajram;
import static com.flipkart.krystal.krystex.kryon.VajramKryonExecutor.GraphTraversalStrategy.BREADTH;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static lombok.AccessLevel.PACKAGE;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.except.KrystalCompletionException;
import com.flipkart.krystal.except.SkippedExecutionException;
import com.flipkart.krystal.except.StackTracingStrategy;
import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.KrystalExecutorConfig;
import com.flipkart.krystal.krystex.KrystalExecutorConfig.KrystalExecutorConfigBuilder;
import com.flipkart.krystal.krystex.KrystexGraph;
import com.flipkart.krystal.krystex.commands.DirectForwardCommand;
import com.flipkart.krystal.krystex.commands.DirectForwardReceive;
import com.flipkart.krystal.krystex.commands.DirectForwardSend;
import com.flipkart.krystal.krystex.commands.ForwardReceiveBatch;
import com.flipkart.krystal.krystex.commands.ForwardSendBatch;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.commands.ServerSideCommand;
import com.flipkart.krystal.krystex.decoration.DecorationOrdering;
import com.flipkart.krystal.krystex.decoration.FlushCommand;
import com.flipkart.krystal.krystex.decoration.FlushableDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecoratorConfig;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyExecutionContext;
import com.flipkart.krystal.krystex.dependencydecorators.TraitDispatchDecorator;
import com.flipkart.krystal.krystex.epochs.EpochGroups;
import com.flipkart.krystal.krystex.internal.KrystalExecutorExecService;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorationInput;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorator;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.kryondecoration.KryonExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecorationContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.flipkart.krystal.krystex.request.IntReqGenerator;
import com.flipkart.krystal.krystex.request.InvocationId;
import com.flipkart.krystal.krystex.request.RequestIdGenerator;
import com.flipkart.krystal.krystex.request.StringReqGenerator;
import com.flipkart.krystal.traits.StaticDispatchPolicy;
import com.flipkart.krystal.traits.TraitDispatchPolicy;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Default implementation of Krystal executor which */
@Slf4j
public final class VajramKryonExecutor implements KrystalExecutor {

  public enum KryonExecStrategy {
    @Deprecated
    BATCH,
    DIRECT;
  }

  @Deprecated
  public enum GraphTraversalStrategy {
    DEPTH,
    BREADTH;
  }

  private static final AtomicLong EXEC_COUNT = new AtomicLong();

  private final KrystexGraph krystexGraph;
  private final KryonDefinitionRegistry kryonDefinitionRegistry;
  private final KrystalExecutorConfig executorConfig;

  private final ExecutorService commandQueue;

  @Getter(PACKAGE)
  private final SingleThreadExecutor singleThreadExecutor;

  @Getter private final String executorId;

  /**
   * We need to have a list of request scope global decorators corresponding to each type, in case
   * we want to have a decorator of one type but based on some config in request, we want to choose
   * one. Ex : Logger, based on prod or preprod env if we want to choose different types of loggers
   * Error logger or info logger
   */
  private final ImmutableMap<String, OutputLogicDecoratorConfig> outputLogicDecoratorConfigs;

  private final ImmutableMap<String, DependencyDecoratorConfig> dependencyDecoratorConfigs;
  private final ImmutableMap<String, KryonDecoratorConfig> kryonDecoratorConfigs;

  private final KryonRegistry<Kryon<?, ?>> kryonRegistry = new KryonRegistry<>();
  private final Map<VajramID, Kryon<?, ?>> decoratedKryons = new LinkedHashMap<>();

  private final Map<VajramID, List<KryonDecorator>> kryonDecorators = new LinkedHashMap<>();
  private final Map<VajramID, List<OutputLogicDecorator>> outputLogicDecoratorsByVajram =
      new LinkedHashMap<>();
  private final KryonExecutorMetrics kryonMetrics;
  private final Map<InvocationId, KryonExecution<?>> allExecutions = new LinkedHashMap<>();

  /**
   * Vajrams invoked directly by using the executors. If a trait is invoked directly, then the
   * vajram that the trait is resolved to is considered to be directly invoked
   */
  private final Set<VajramID> directlyInvokedVajrams = new LinkedHashSet<>();

  private final RequestIdGenerator preferredReqGenerator;
  @Getter private final KrystalExecutorExecutionInfo executionInfo;

  private @MonotonicNonNull List<FlushableDecorator> flushableDecorators;

  private volatile boolean closed;
  private boolean shutdownRequested;

  public VajramKryonExecutor(
      KrystexGraph krystexGraph,
      @CalledMethods("executorService") KrystalExecutorConfigBuilder executorConfigBuilder) {
    this.executorConfig = executorConfigBuilder.build();
    this.kryonDefinitionRegistry = krystexGraph.vajramGraph().kryonDefinitionRegistry();
    this.krystexGraph = krystexGraph;
    this.singleThreadExecutor = executorConfig.executorService();
    this.executorId =
        requireNonNullElseGet(
            executorConfig.executorId(), () -> "KrystalExecutor-" + EXEC_COUNT.getAndIncrement());
    this.outputLogicDecoratorConfigs =
        ImmutableMap.copyOf(executorConfig.outputLogicDecoratorConfigs());
    this.dependencyDecoratorConfigs =
        ImmutableMap.copyOf(executorConfig.dependencyDecoratorConfigs());
    this.kryonDecoratorConfigs = ImmutableMap.copyOf(executorConfig.kryonDecoratorConfigs());
    this.executionInfo = executorConfig.executorInfo();
    this.kryonMetrics = new KryonExecutorMetrics();
    this.preferredReqGenerator =
        executorConfig.debug() ? new StringReqGenerator() : new IntReqGenerator();

    // Suppress checker-framework errors caused by passing "this" to KrystalExecutorExecService.
    // This is not an issue here because this is the last thing we are doing before exiting the
    // constructor
    @SuppressWarnings({"assignment", "argument"})
    @Initialized
    KrystalExecutorExecService decoratedExecService =
        new KrystalExecutorExecService(
            this,
            executorConfig.executorServiceTransformer().apply(executorConfig.executorService()));
    this.commandQueue = decoratedExecService;
  }

  private List<OutputLogicDecorator> getSortedOutputLogicDecorators(VajramID vajramID) {
    return outputLogicDecoratorsByVajram.computeIfAbsent(
        vajramID,
        _k -> {
          KryonDefinition kryonDefinition = kryonDefinitionRegistry.getOrThrow(vajramID);
          if (kryonDefinition instanceof TraitKryonDefinition) {
            // Traits don't have an output logic - so no output logic decorators
            return List.of();
          }
          VajramKryonDefinition vajramKryonDefinition =
              validateAsVajram(kryonDefinitionRegistry.getOrThrow(vajramID));
          LogicDecorationContext logicDecorationContext =
              new LogicDecorationContext(
                  vajramID,
                  vajramKryonDefinition.getOutputLogicDefinition().tags(),
                  vajramKryonDefinition.kryonDefinitionRegistry());
          DecorationOrdering decorationOrdering = executorConfig.decorationOrdering();
          ImmutableMap<String, Integer> decoratorIndices =
              decorationOrdering.outputLogicDecoratorIndices();
          List<OutputLogicDecorator> decoratorsWithNoIndex = new ArrayList<>();

          // Use radix sort for quick sorting
          List<@Nullable OutputLogicDecorator> radixSortedDecorators =
              new ArrayList<>(decoratorIndices.size());
          decoratorIndices.forEach((s, integer) -> radixSortedDecorators.add(null));

          outputLogicDecoratorConfigs.forEach(
              (decoratorType, decoratorConfig) -> {
                if (decoratorConfig.shouldDecorate().test(logicDecorationContext)) {
                  OutputLogicDecorator outputLogicDecorator =
                      decoratorConfig.factory().apply(logicDecorationContext);
                  if (outputLogicDecorator == null) {
                    return;
                  }
                  Integer index = decoratorIndices.get(decoratorType);
                  if (index == null) {
                    decoratorsWithNoIndex.add(outputLogicDecorator);
                  } else {
                    radixSortedDecorators.set(index, outputLogicDecorator);
                  }
                }
              });
          List<OutputLogicDecorator> sortedDecorators =
              new ArrayList<>(decoratorsWithNoIndex.size() + radixSortedDecorators.size());
          sortedDecorators.addAll(decoratorsWithNoIndex); // decorators with no index go first
          for (OutputLogicDecorator decorator : radixSortedDecorators) {
            // Filter out nulls in radix sorted list
            if (decorator != null) {
              sortedDecorators.add(decorator);
            }
          }
          return unmodifiableList(sortedDecorators);
        });
  }

  private List<DependencyDecorator> getDependencyDecorators(
      DependencyExecutionContext dependencyExecutionContext) {
    List<DependencyDecorator> decorators = new ArrayList<>();
    for (DependencyDecoratorConfig decoratorConfig : dependencyDecoratorConfigs.values()) {
      if (decoratorConfig.shouldDecorate().test(dependencyExecutionContext)) {
        decorators.add(decoratorConfig.factory().apply(dependencyExecutionContext));
      }
    }
    return unmodifiableList(decorators);
  }

  @Override
  @SuppressWarnings("FutureReturnValueIgnored")
  public <T> CompletableFuture<@Nullable T> execute(
      ImmutableRequest<T> request, VajramExecutionConfig executionConfig) {
    RequestResponseFuture<Request<T>, T> requestResponseFuture = forRequest(request);
    execute(requestResponseFuture, executionConfig);
    return requestResponseFuture.response();
  }

  @Override
  public <T> void execute(
      RequestResponseFuture<? extends Request<T>, T> requestResponseFuture,
      VajramExecutionConfig executionConfig) {
    @SuppressWarnings("unchecked")
    ImmutableRequest<Object> castRequest =
        (ImmutableRequest<Object>) requestResponseFuture.request()._build();
    if (closed) {
      throw new RejectedExecutionException("KryonExecutor is already closed");
    }
    checkArgument(executionConfig != null, "executionConfig can not be null");
    VajramID vajramID = castRequest._vajramID();
    @SuppressWarnings("TestOnlyProblems")
    boolean openAllKryonsForExternalInvocation =
        Boolean.parseBoolean(
            System.getProperty(RISKY_OPEN_ALL_VAJRAMS_TO_EXTERNAL_INVOCATION_PROP_NAME, "false"));
    if (!krystexGraph.externallyInvocableVajramIds().contains(vajramID)
        && !openAllKryonsForExternalInvocation) {
      throw new RejectedExecutionException(
          "Invocation from outside krystal graph has not been enabled for vajramId: " + vajramID);
    }

    String executionId = executionConfig.executionId();
    checkArgument(executionId != null, "executionConfig.executionId can not be null");

    enqueueRunnable(
        // Perform all data-structure manipulations in the command queue
        // to avoid multi-thread access
        () -> {
          try {
            VajramID resolvedVajramId =
                resolveDispatchTarget(vajramID, castRequest, executionConfig);
            if (resolvedVajramId == null) {
              requestResponseFuture
                  .response()
                  .completeExceptionally(
                      new KrystalCompletionException(
                          "Trait dispatch policy resolved vajramID 'null' for Trait Id "
                              + vajramID));
              return;
            }
            InvocationId invocationId =
                preferredReqGenerator.newRequest(() -> "%s:%s".formatted(executorId, executionId));
            if (allExecutions.containsKey(invocationId)) {
              requestResponseFuture
                  .response()
                  .completeExceptionally(
                      wrapAsCompletionException(
                          new IllegalArgumentException(
                              "Received duplicate requests for same instanceId '%s' and execution Id '%s'"
                                  .formatted(executorId, executionId))));
            } else {
              directlyInvokedVajrams.add(resolvedVajramId);
              allExecutions.put(
                  invocationId,
                  new KryonExecution<>(
                      resolvedVajramId, invocationId, requestResponseFuture, executionConfig));
            }
          } catch (Throwable e) {
            requestResponseFuture.response().completeExceptionally(e);
          }
        });
  }

  private @Nullable VajramID resolveDispatchTarget(
      VajramID vajramID, ImmutableRequest<Object> request, VajramExecutionConfig executionConfig) {
    KryonDefinition kryonDefinition = kryonDefinitionRegistry.getOrThrow(vajramID);
    if (!(kryonDefinition instanceof TraitKryonDefinition)) {
      return vajramID;
    }
    TraitDispatchPolicy traitDispatchPolicy = getTraitDispatchPolicyForTrait(vajramID);
    VajramID resolvedVajramId;
    if (traitDispatchPolicy instanceof StaticDispatchPolicy staticDispatchPolicy) {
      resolvedVajramId =
          staticDispatchPolicy.getDispatchTargetID(executionConfig.staticDispatchQualifier());
    } else {
      resolvedVajramId = traitDispatchPolicy.getDispatchTargetID(null, request);
    }
    return resolvedVajramId;
  }

  private TraitDispatchPolicy getTraitDispatchPolicyForTrait(VajramID vajramID) {
    TraitDispatchDecorator traitDispatchDecorator = getTraitDispatchDecorator();
    @Nullable TraitDispatchPolicy traitDispatchPolicy =
        traitDispatchDecorator.traitDispatchPolicies().get(vajramID);
    if (traitDispatchPolicy == null) {
      throw new IllegalArgumentException(
          "Trait "
              + vajramID
              + " found but no TraitDispatchPolicy provided in the executorConfig or KrystexGraph");
    }
    return traitDispatchPolicy;
  }

  private TraitDispatchDecorator getTraitDispatchDecorator() {
    TraitDispatchDecorator traitDispatchDecorator = executorConfig.traitDispatchDecorator();
    if (traitDispatchDecorator == null) {
      traitDispatchDecorator =
          requireNonNullElse(krystexGraph.traitDispatchDecorator(), TraitDispatchDecorator.NO_OP);
    }
    return traitDispatchDecorator;
  }

  private Kryon<?, ?> createKryonIfAbsent(VajramID vajramID) {
    return kryonRegistry.createIfAbsent(
        vajramID,
        _n -> {
          VajramKryonDefinition kryonDefinition =
              validateAsVajram(kryonDefinitionRegistry.getOrThrow(vajramID));
          return switch (executorConfig.kryonExecStrategy()) {
            case BATCH ->
                new BatchKryon(
                    kryonDefinition,
                    this,
                    this::getSortedOutputLogicDecorators,
                    this::getDependencyDecorators,
                    executorConfig.decorationOrdering(),
                    preferredReqGenerator);
            case DIRECT ->
                new DirectKryon(
                    kryonDefinition,
                    this,
                    this::getSortedOutputLogicDecorators,
                    this::getDependencyDecorators,
                    executorConfig.decorationOrdering());
          };
        });
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
      Supplier<? extends KryonCommand<? extends R>> kryonCommand) {
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
   * command is originating from the same main thread inside the command queue. This avoids the
   * potentially unnecessary contention in the thread-safe structures inside the command-queue.
   */
  <R extends KryonCommandResponse> CompletableFuture<R> executeCommand(
      KryonCommand<R> kryonCommand) {
    if (BREADTH.equals(executorConfig.graphTraversalStrategy())) {
      return enqueueKryonCommand(() -> kryonCommand);
    } else {
      kryonMetrics.commandQueueBypassed();
      return _executeCommand(kryonCommand);
    }
  }

  private <R extends KryonCommandResponse> CompletableFuture<R> _executeCommand(
      KryonCommand<? extends R> kryonCommand) {
    VajramID previousActiveVajram = executionInfo.activeVajram();
    try {
      DependentChain dependentChain = kryonCommand.dependentChain();
      VajramKryonDefinition vajramKryonDefinition =
          validateAsVajram(kryonDefinitionRegistry.getOrThrow(kryonCommand.vajramID()));
      if (!(kryonCommand instanceof ServerSideCommand<? extends R>)) {
        if (kryonCommand instanceof DirectForwardSend forwardSend) {
          List<ExecutionItem> list = new ArrayList<>();
          for (RequestResponseFuture<? extends Request<?>, ?> executableRequest :
              forwardSend.executableRequests()) {
            @SuppressWarnings("unchecked")
            CompletableFuture<@Nullable Object> response =
                (CompletableFuture<@Nullable Object>) executableRequest.response();
            list.add(
                new ExecutionItem(
                    vajramKryonDefinition
                        .facetsFromRequest()
                        .logic()
                        .facetsFromRequest(executableRequest.request()),
                    response));
          }

          //noinspection unchecked
          return _executeCommand(
              (KryonCommand<? extends R>)
                  DirectForwardCommand.ofExecutionItems(
                      forwardSend.vajramID(), list, dependentChain));
        } else if (kryonCommand instanceof ForwardSendBatch forwardSend) {
          //noinspection unchecked
          return (CompletableFuture<R>)
              _executeCommand(
                  new ForwardReceiveBatch(
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
                      dependentChain));
        }
      }
      if (shouldSkip(kryonCommand)) {
        flushDescendents(kryonCommand.dependentChain(), kryonCommand.vajramID());
        return failedFuture(new SkippedExecutionException("Skipping since there are no requests"));
      } else if (isDepChainDisabled(dependentChain)) {
        log.info(
            "Returning empty response since dependentChain {} has been disabled", dependentChain);
        // Throwing exception here is causing extreme CPU wastage due to JIT deoptimization.
        // So we use the exception to fail the futures instead.
        Exception exception = new DisabledDependentChainException(dependentChain);
        kryonCommand.error(exception);
        return failedFuture(exception);
      }
      validate();
      VajramID vajramID = kryonCommand.vajramID();
      Kryon<KryonCommand<? extends R>, R> kryon = getDecoratedKryon(vajramID);
      executionInfo.activeVajram(kryon.getKryonDefinition().vajramID());
      return kryon.executeCommand(kryonCommand);
    } catch (Throwable e) {
      kryonCommand.error(e);
      return failedFuture(e);
    } finally {
      executionInfo.activeVajram(previousActiveVajram);
    }
  }

  private void flushDescendents(DependentChain ancestor, VajramID fromVajramId) {
    if (flushableDecorators == null) {
      List<FlushableDecorator> flushableCollector = new ArrayList<>();
      for (VajramDefinition targetVajramDef :
          krystexGraph.vajramGraph().vajramDefinitions().values()) {
        if (targetVajramDef.isTrait()) {
          continue;
        }
        VajramID targetVajram = targetVajramDef.vajramId();
        List<KryonDecorator> sortedKryonDecorators = getSortedKryonDecorators(targetVajram);
        for (KryonDecorator kryonDecorator : sortedKryonDecorators) {
          if (kryonDecorator instanceof FlushableDecorator flushableDecorator) {
            flushableCollector.add(flushableDecorator);
          }
        }
        for (OutputLogicDecorator outputLogicDecorator :
            getSortedOutputLogicDecorators(targetVajram)) {
          if (outputLogicDecorator instanceof FlushableDecorator flushableDecorator) {
            flushableCollector.add(flushableDecorator);
          }
        }
      }
      this.flushableDecorators = unmodifiableList(flushableCollector);
    }
    for (FlushableDecorator flushableDecorator : flushableDecorators) {
      flushableDecorator.flushDecorator(new FlushCommand(ancestor, fromVajramId));
    }
  }

  private <R extends KryonCommandResponse> boolean shouldSkip(
      KryonCommand<? extends R> kryonCommand) {
    if (kryonCommand instanceof DirectForwardReceive directForwardReceive) {
      return directForwardReceive.shouldSkip();
    }
    if (kryonCommand instanceof ForwardSendBatch forwardSend) {
      return forwardSend.shouldSkip();
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private <R extends KryonCommandResponse> Kryon<KryonCommand<? extends R>, R> getDecoratedKryon(
      VajramID vajramID) {
    return (Kryon<KryonCommand<? extends R>, R>)
        decoratedKryons.computeIfAbsent(
            vajramID,
            _n -> {
              Kryon<?, ?> kryon = createKryonIfAbsent(vajramID);
              for (KryonDecorator kryonDecorator :
                  Lists.reverse(getSortedKryonDecorators(vajramID))) {
                kryon =
                    kryonDecorator.decorateKryon(
                        new KryonDecorationInput(
                            (Kryon<KryonCommand<?>, KryonCommandResponse>) kryon, this));
              }
              return kryon;
            });
  }

  private List<KryonDecorator> getSortedKryonDecorators(VajramID vajramID) {
    return kryonDecorators.computeIfAbsent(
        vajramID,
        _k -> {
          KryonExecutionContext executionContext = new KryonExecutionContext(vajramID);
          DecorationOrdering decorationOrdering = executorConfig.decorationOrdering();
          ImmutableMap<String, Integer> kryonDecoratorIndices =
              decorationOrdering.kryonDecoratorIndices();
          List<KryonDecorator> radixSortedDecorators =
              new ArrayList<>(kryonDecoratorIndices.size());
          kryonDecoratorIndices.forEach((_d, _i) -> radixSortedDecorators.add(null));
          List<KryonDecorator> decoratorsWithNoIndex = new ArrayList<>();
          for (Entry<String, KryonDecoratorConfig> configsByType :
              kryonDecoratorConfigs.entrySet()) {
            String decoratorType = configsByType.getKey();
            KryonDecoratorConfig decoratorConfig = configsByType.getValue();
            if (!decoratorConfig.shouldDecorate().test(executionContext)) {
              continue;
            }
            KryonDecorator kryonDecorator = decoratorConfig.factory().apply(executionContext);
            if (kryonDecorator == null) {
              continue;
            }
            Integer index = kryonDecoratorIndices.get(decoratorType);
            if (index == null) {
              decoratorsWithNoIndex.add(kryonDecorator);
            } else {
              radixSortedDecorators.set(index, kryonDecorator);
            }
          }
          List<KryonDecorator> sortedDecorators =
              new ArrayList<>(radixSortedDecorators.size() + decoratorsWithNoIndex.size());
          sortedDecorators.addAll(decoratorsWithNoIndex);
          for (KryonDecorator kryonDecorator : radixSortedDecorators) {
            if (kryonDecorator != null) {
              sortedDecorators.add(kryonDecorator);
            }
          }
          return unmodifiableList(sortedDecorators);
        });
  }

  private void validate() {
    if (shutdownRequested) {
      throw new RejectedExecutionException("Kryon Executor shutdown requested.");
    }
  }

  private boolean isDepChainDisabled(DependentChain dependentChain) {
    return krystexGraph.dependentChainDisabler().isDisabled(dependentChain);
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void flush() {
    enqueueRunnable(
        () -> {
          switch (executorConfig.kryonExecStrategy()) {
            case BATCH -> submitBatch(allExecutions.values());
            case DIRECT -> submitDirect(allExecutions.values());
          }
          DependentChainStart dependentChainsStart =
              kryonDefinitionRegistry.getDependentChainsStart();
          // For those vajrams which are could have been invoked directly, but were not,
          // Flush all the descendants to prune that part of the graph
          ImmutableMap<VajramID, EpochGroups> directlyInvocableVajrams =
              krystexGraph
                  .epochGroupsByAncestors()
                  .epochGroupsFromVajram()
                  .getOrDefault(dependentChainsStart, ImmutableMap.of());
          for (VajramID directlyInvocableVajram : directlyInvocableVajrams.keySet()) {
            if (!directlyInvokedVajrams.contains(directlyInvocableVajram)) {
              flushDescendents(dependentChainsStart, directlyInvocableVajram);
            }
          }
        });
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void submitDirect(Collection<KryonExecution<?>> allExecutions) {
    Map<VajramID, List<KryonExecution<?>>> executionsByKryon = new HashMap<>();
    for (KryonExecution<?> anExecution : allExecutions) {
      executionsByKryon
          .computeIfAbsent(anExecution.vajramID(), k -> new ArrayList<>())
          .add(anExecution);
    }
    executionsByKryon.forEach(
        (vajramID, kryonExecutions) -> {
          try {
            CompletableFuture<DirectResponse> submissionResponse =
                this.executeCommand(
                    new DirectForwardCommand(
                        vajramID,
                        asRequestResponseFutures(kryonExecutions),
                        kryonDefinitionRegistry.getDependentChainsStart()));
            submissionResponse.whenComplete(
                (response, throwable) -> {
                  if (throwable != null) {
                    for (KryonExecution<?> kryonExecution : kryonExecutions) {
                      kryonExecution
                          .response()
                          .completeExceptionally(wrapAsCompletionException(throwable));
                    }
                  }
                });
          } catch (Throwable throwable) {
            for (KryonExecution<?> ke : kryonExecutions) {
              ke.response().completeExceptionally(wrapAsCompletionException(throwable));
            }
          }
        });
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void submitBatch(Collection<KryonExecution<?>> allExecutions) {
    Map<VajramID, List<KryonExecution<?>>> executionsByKryon =
        allExecutions.stream().collect(groupingBy(KryonExecution::vajramID));
    executionsByKryon.forEach(
        (vajramID, kryonExecutions) -> {
          CompletableFuture<BatchResponse> batchResponseFuture;
          try {
            Map<InvocationId, Request<Object>> requests =
                new LinkedHashMap<>(kryonExecutions.size());
            for (KryonExecution<?> kryonExecution : kryonExecutions) {
              requests.put(kryonExecution.instanceExecutionId(), kryonExecution.request());
            }
            batchResponseFuture =
                this.executeCommand(
                    new ForwardSendBatch(
                        vajramID, requests, kryonDefinitionRegistry.getDependentChainsStart()));
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
                    for (KryonExecution<?> kryonExecution : kryonExecutions) {
                      if (throwable != null) {
                        kryonExecution
                            .requestResponseFuture()
                            .response()
                            .completeExceptionally(wrapAsCompletionException(throwable));
                      } else {
                        linkFutures(
                            responses
                                .getOrDefault(kryonExecution.instanceExecutionId(), Errable.nil())
                                .toFuture(),
                            kryonExecution.response());
                      }
                    }
                  });
          propagateCancellation(
              allOf(
                  kryonExecutions.stream()
                      .map(KryonExecution::response)
                      .toArray(CompletableFuture[]::new)),
              batchResponseFuture);
        });
  }

  private List<RequestResponseFuture<? extends Request<?>, ?>> asRequestResponseFutures(
      List<KryonExecution<?>> kryonExecutions) {
    List<RequestResponseFuture<? extends Request<?>, ?>> list =
        new ArrayList<>(kryonExecutions.size());
    kryonExecutions.forEach(ke -> list.add(ke.requestResponseFuture()));
    return list;
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
        () -> {
          Collection<KryonExecution<?>> executions = allExecutions.values();
          CompletableFuture[] responseFutures = new CompletableFuture[executions.size()];
          int i = 0;
          for (KryonExecution<?> kryonExecution : executions) {
            responseFutures[i++] = kryonExecution.response();
          }
          return allOf(responseFutures)
              .whenComplete(
                  (unused, throwable) ->
                      outputLogicDecoratorsByVajram.forEach(
                          (vajramID, outputLogicDecorators) ->
                              outputLogicDecorators.forEach(
                                  KrystalExecutorCompletionListener::onComplete)));
        });
  }

  @Override
  public void shutdownNow() {
    _close0();
    this.shutdownRequested = true;
  }

  ExecutorService commandQueue() {
    return commandQueue;
  }

  private void _close0() {
    this.closed = true;
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void enqueueRunnable(Runnable command) {
    enqueueCommand(
        () -> {
          command.run();
          return null;
        });
  }

  private <T> CompletableFuture<T> enqueueCommand(Supplier<T> command) {
    return supplyAsync(
        decorateTask(
            () -> {
              kryonMetrics.commandQueued();
              return command.get();
            }),
        commandQueue());
  }

  private <T> Supplier<T> decorateTask(Supplier<T> task) {
    return configureStackTracing(task);
  }

  private <T> Supplier<T> configureStackTracing(Supplier<T> task) {
    if (executorConfig.debug()) {
      return () -> {
        StackTracingStrategy oldValue = setStackTracingStrategyForCurrentThread(FILL);
        try {
          return task.get();
        } finally {
          setStackTracingStrategyForCurrentThread(oldValue);
        }
      };
    } else {
      return task;
    }
  }

  @SuppressWarnings("unchecked")
  private record KryonExecution<T>(
      VajramID vajramID,
      InvocationId instanceExecutionId,
      RequestResponseFuture<? extends Request<T>, T> requestResponseFuture,
      VajramExecutionConfig executionConfig) {

    private CompletableFuture<@Nullable Object> response() {
      return (CompletableFuture<@Nullable Object>) requestResponseFuture().response();
    }

    private Request<Object> request() {
      return (Request<Object>) requestResponseFuture().request();
    }
  }
}
