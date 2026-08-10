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
import static java.util.Collections.unmodifiableSet;
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
import com.flipkart.krystal.except.StackTracingStrategy;
import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.KrystalExecutorConfig;
import com.flipkart.krystal.krystex.KrystalExecutorConfig.KrystalExecutorConfigBuilder;
import com.flipkart.krystal.krystex.KrystexGraph;
import com.flipkart.krystal.krystex.commands.DirectForwardCommand;
import com.flipkart.krystal.krystex.commands.DirectForwardSend;
import com.flipkart.krystal.krystex.commands.ForwardReceiveBatch;
import com.flipkart.krystal.krystex.commands.ForwardSendBatch;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.commands.ServerSideCommand;
import com.flipkart.krystal.krystex.decoration.InitiateActiveDepChains;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecoratorConfig;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyExecutionContext;
import com.flipkart.krystal.krystex.dependencydecorators.TraitDispatchDecorator;
import com.flipkart.krystal.krystex.internal.KrystalExecutorExecService;
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
import com.flipkart.krystal.traits.StaticDispatchPolicy;
import com.flipkart.krystal.traits.TraitDispatchPolicy;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Default implementation of Krystal executor which */
@Slf4j
public final class VajramKryonExecutor implements KrystalExecutor {

  public enum KryonExecStrategy {
    BATCH,
    DIRECT;
  }

  public enum GraphTraversalStrategy {
    DEPTH,
    BREADTH;
  }

  private static final AtomicLong EXEC_COUNT = new AtomicLong();

  private final KrystexGraph krystexGraph;
  private final KryonDefinitionRegistry kryonDefinitionRegistry;
  private final KrystalExecutorConfig executorConfig;
  private @MonotonicNonNull Set<DependentChain> disabledDependentChainsForExecutor;

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

  private final KryonRegistry<Kryon<?, ?>> kryonRegistry = new KryonRegistry<>();
  private final Map<VajramID, Kryon<?, ?>> decoratedKryons = new HashMap<>();
  private final KryonExecutorMetrics kryonMetrics;
  private final Map<InvocationId, KryonExecution<?>> allExecutions = new LinkedHashMap<>();

  /**
   * Vajrams invoked directly by using the executors. If a trait is invoked directly, then the
   * vajram that the trait is resolved to is considered to be directly invoked
   */
  private final Set<VajramID> directlyInvokedVajrams = new LinkedHashSet<>();

  private final Map<VajramID, ImmutableSet<DependentChain>> dependentChainsPerKryon =
      new LinkedHashMap<>();
  private final RequestIdGenerator preferredReqGenerator;
  @Getter private final KrystalExecutorExecutionInfo executionInfo;

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

  private NavigableSet<OutputLogicDecorator> getOutputLogicDecorators(
      LogicExecutionContext logicExecutionContext) {
    VajramID vajramID = logicExecutionContext.vajramID();
    TreeSet<OutputLogicDecorator> decorators =
        new TreeSet<>(executorConfig.decorationOrdering().encounterOrder().reversed());
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
                              new InitiateActiveDepChains(vajramID, getDependentChains(vajramID)));
                          return logicDecorator;
                        });
            decorators.add(outputLogicDecorator);
          }
        });
    return decorators;
  }

  private ImmutableSet<DependentChain> getDependentChains(VajramID vajramID) {
    return dependentChainsPerKryon.computeIfAbsent(
        vajramID,
        _v -> {
          Set<@NonNull DependentChain> depChainsForVajram =
              krystexGraph.dependentChainsByVajram().getOrDefault(vajramID, Set.of());
          return ImmutableSet.copyOf(
              Sets.filter(
                  depChainsForVajram,
                  depChain -> {
                    VajramID firstVajram = depChain.getFirstVajram();
                    if (firstVajram == null) {
                      // This means that `depChain` is a DependentChainStart
                      // which means `vajramId` has been configured so that it can be invoked
                      // directly - so we should consider vajramId itself as the first vajram
                      firstVajram = vajramID;
                    }
                    if (!directlyInvokedVajrams.contains(firstVajram)) {
                      // Only consider those dependent chains which start with an vajram invoked
                      // directly
                      return false;
                    }
                    for (DependentChain disabledDepChain : disabledDependentChainsForExecutor()) {
                      if (depChain.startsWith(disabledDepChain)) {
                        return false;
                      }
                    }
                    return true;
                  }));
        });
  }

  /**
   * Returns those dependent chains which have been disabled at the executor level, or have been
   * disabled on all the individual executions in this executor.
   */
  private Set<DependentChain> disabledDependentChainsForExecutor() {
    if (disabledDependentChainsForExecutor == null) {
      Set<DependentChain> depChainsDisabledInAllExecutions = new LinkedHashSet<>();
      {
        // Find those dependentChains which are disabled in ALL the execution-level disabled chains.
        // i.e. Find intersection of all sets in the list disabledDependentChainsPerExecution

        // Algo: Take the first set, and call retainAll for all other sets
        boolean first = true;
        for (KryonExecution<?> kryonExecution : allExecutions.values()) {
          ImmutableSet<DependentChain> disabledDepChains =
              kryonExecution.executionConfig().disabledDependentChains();
          if (first) {
            depChainsDisabledInAllExecutions.addAll(disabledDepChains);
            first = false;
          } else {
            if (depChainsDisabledInAllExecutions.isEmpty()) {
              break;
            }
            depChainsDisabledInAllExecutions.retainAll(disabledDepChains);
          }
        }
      }
      // executorConfig level disabledDependentChains are considered disabled
      depChainsDisabledInAllExecutions.addAll(executorConfig.disabledDependentChains());
      disabledDependentChainsForExecutor = unmodifiableSet(depChainsDisabledInAllExecutions);
    }
    return disabledDependentChainsForExecutor;
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
                    this::getOutputLogicDecorators,
                    this::getDependencyDecorators,
                    executorConfig.decorationOrdering(),
                    preferredReqGenerator);
            case DIRECT ->
                new DirectKryon(
                    kryonDefinition,
                    this,
                    this::getOutputLogicDecorators,
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
                      forwardSend.vajramID(), list, forwardSend.dependentChain()));

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
                      forwardSend.dependentChain()));
        }
      }
      validate(kryonCommand);
      VajramID vajramID = kryonCommand.vajramID();
      Kryon<KryonCommand<? extends R>, R> kryon = getDecoratedKryon(kryonCommand, vajramID);
      executionInfo.activeVajram(kryon.getKryonDefinition().vajramID());
      return kryon.executeCommand(kryonCommand);
    } catch (Throwable e) {
      kryonCommand.error(e);
      return failedFuture(e);
    } finally {
      executionInfo.activeVajram(previousActiveVajram);
    }
  }

  @SuppressWarnings("unchecked")
  private <R extends KryonCommandResponse> Kryon<KryonCommand<? extends R>, R> getDecoratedKryon(
      KryonCommand<?> kryonCommand, VajramID vajramID) {
    return (Kryon<KryonCommand<? extends R>, R>)
        decoratedKryons.computeIfAbsent(
            vajramID,
            _n -> {
              Kryon<?, ?> kryon = createKryonIfAbsent(vajramID);
              for (KryonDecorator kryonDecorator :
                  getSortedKryonDecorators(vajramID, kryonCommand)) {
                kryon =
                    kryonDecorator.decorateKryon(
                        new KryonDecorationInput(
                            (Kryon<KryonCommand<?>, KryonCommandResponse>) kryon, this));
              }
              return kryon;
            });
  }

  private Set<KryonDecorator> getSortedKryonDecorators(
      VajramID vajramID, KryonCommand<?> kryonCommand) {
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

  private void validate(KryonCommand<?> kryonCommand) {
    if (shutdownRequested) {
      throw new RejectedExecutionException("Kryon Executor shutdown requested.");
    }
    DependentChain dependentChain = kryonCommand.dependentChain();
    if (krystexGraph.dependentChainDisabler().isDisabled(dependentChain)
        || disabledDependentChainsForExecutor().contains(dependentChain)) {
      throw new DisabledDependentChainException(dependentChain);
    }
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void flush() {
    enqueueRunnable(
        () -> {
          switch (executorConfig.kryonExecStrategy()) {
            case BATCH -> submitBatch(allExecutions.values());
            case DIRECT -> submitDirect(allExecutions.values());
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
                  });
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
