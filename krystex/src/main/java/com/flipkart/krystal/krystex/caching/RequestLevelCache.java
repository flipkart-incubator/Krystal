package com.flipkart.krystal.krystex.caching;

import static com.flipkart.krystal.concurrent.Futures.linkFutures;
import static com.flipkart.krystal.concurrent.Futures.propagateCompletion;
import static com.flipkart.krystal.data.DataAccess.AccessPattern.MUTATION;
import static com.flipkart.krystal.data.DataAccess.AccessPattern.QUERY;
import static com.flipkart.krystal.except.KrystalCompletionException.wrapAsCompletionException;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.stream.Collectors.toUnmodifiableSet;

import com.flipkart.krystal.annos.ComputeDelegationMode;
import com.flipkart.krystal.annos.OutputLogicDelegationMode;
import com.flipkart.krystal.core.OutputLogicExecutionInput;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.DataAccess;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.ImmutableFacetValues;
import com.flipkart.krystal.except.KrystalCompletionException;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.commands.DirectForwardCommand;
import com.flipkart.krystal.krystex.commands.DirectForwardReceive;
import com.flipkart.krystal.krystex.commands.ForwardReceiveBatch;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.BatchResponse;
import com.flipkart.krystal.krystex.kryon.Kryon;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.krystex.kryon.VajramKryonDefinition;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorationInput;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorator;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.flipkart.krystal.krystex.request.InvocationId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public sealed class RequestLevelCache permits TestRequestLevelCache {

  public static final String KRYON_DECORATOR_TYPE =
      RequestLevelCache.class.getName() + "_KryonDecorator";
  public static final String OUTPUT_LOGIC_DECORATOR_TYPE =
      RequestLevelCache.class.getName() + "_OutputLogicDecorator";

  private static final Errable<Object> UNKNOWN_ERROR =
      Errable.withError(new KrystalCompletionException("Unknown error in request cache"));

  private final CacheContainer cache = new CacheContainer();

  @Getter private final VajramGraph vajramGraph;
  private @MonotonicNonNull KryonDecorator kryonDecorator;
  private @MonotonicNonNull OutputLogicDecorator outputLogicDecorator;

  /**
   * If a vajram doesn't have a @DataAccess annotation, then it is assumed that the vajram does not
   * mutate any state.
   *
   * @param vajramGraph the VajramGraph corresponding to the Krystal executor for which this is a
   *     request level cache
   */
  public RequestLevelCache(VajramGraph vajramGraph) {
    this.vajramGraph = vajramGraph;
  }

  /**
   * Returns a configurator which configures the provided configBuilder such that all eligible
   * computeVajrams are cached using a KryonDecorator, and all eligible IOVajrams are cached using
   * an OutputLogicDecorator,
   *
   * <p>The caching KryonDecorator helps avoid executing the complete call graph of a compute vajram
   * if it already has been computed.
   *
   * <p>For IO vajrams we don't use the KryonDecorator because it might choose to skip caching if
   * the kryon directly or indirectly reads queries an entity which is mutated by another vajram. In
   * such cases, the output logic decorator is more robust.
   */
  public KryonExecutorConfigurator defaultDecorationStrategy() {
    return configBuilder ->
        configBuilder
            .configureWith(kryonDecorationForComputeVajrams())
            .configureWith(outputLogicDecorationForIOVajrams());
  }

  public KryonExecutorConfigurator kryonDecorationForComputeVajrams() {
    return configBuilder ->
        configBuilder.kryonDecoratorConfig(
            new KryonDecoratorConfig(
                KRYON_DECORATOR_TYPE,
                executionContext -> {
                  KryonCachingMetadata kryonCachingMetadata =
                      getKryonCachingMetadata(executionContext.vajramID());
                  return kryonCachingMetadata.isComputeVajram()
                      && kryonCachingMetadata.isEligibleForCaching();
                }, // Apply kryon level cache to only compute vajrams
                _c -> KRYON_DECORATOR_TYPE, // Only one RequestLevelCache across the vajram graph
                _c -> kryonDecorator() // Reuse this instance across the graph
                ));
  }

  /**
   * IMPORTANT: If you are using an Input Batching Decorator, or any such decorator which group
   * dependent Chains and block one dependentChain for another, this decorator should have a higher
   * decoration index (i.e. must decorate AFTER) such a decorator, else we can end up with deadlocks
   * in graph execution.
   */
  public KryonExecutorConfigurator outputLogicDecorationForIOVajrams() {
    return configBuilder ->
        configBuilder.outputLogicDecoratorConfig(
            new OutputLogicDecoratorConfig(
                OUTPUT_LOGIC_DECORATOR_TYPE,
                executionContext -> {
                  KryonCachingMetadata kryonCachingMetadata =
                      getKryonCachingMetadata(executionContext.vajramID());
                  return !kryonCachingMetadata.isComputeVajram()
                      && kryonCachingMetadata.isEligibleForCaching();
                }, // Apply output logic level cache only for IO vajrams
                _c -> OUTPUT_LOGIC_DECORATOR_TYPE, // Only one RequestLevelCache across the vajram
                // graph
                _c -> outputLogicDecorator() // Reuse this instance across the graph
                ));
  }

  public KryonDecorator kryonDecorator() {
    if (kryonDecorator == null) {
      kryonDecorator = CachingDecoratedKryon::new;
    }
    return kryonDecorator;
  }

  public OutputLogicDecorator outputLogicDecorator() {
    if (outputLogicDecorator == null) {
      outputLogicDecorator = CachingDecoratedLogic::new;
    }
    return outputLogicDecorator;
  }

  private KryonCachingMetadata getKryonCachingMetadata(VajramID vajramID) {
    return vajramGraph
        .kryonDefinitionRegistry()
        .getOrThrow(vajramID)
        .getCustomMetadata(
            KryonCachingMetadata.class,
            kryonDefinition -> computeCachingMetadata(kryonDefinition, vajramGraph));
  }

  static KryonCachingMetadata computeCachingMetadata(
      KryonDefinition kryonDefinition, VajramGraph vajramGraph) {
    return new KryonCachingMetadata(
        ComputeDelegationMode.NONE
            == kryonDefinition
                .allTags()
                .getAnnotationByType(OutputLogicDelegationMode.class)
                .map(OutputLogicDelegationMode::value)
                .orElse(ComputeDelegationMode.NONE),
        isEligibleForCaching(kryonDefinition, vajramGraph),
        getEntitiesRead(kryonDefinition),
        getEntitiesMutated(kryonDefinition));
  }

  private static Set<String> getEntitiesRead(KryonDefinition kryonDefinition) {
    return kryonDefinition.allTags().getAnnotationsByType(DataAccess.class).stream()
        .filter(h -> QUERY.equals(h.accessPattern()))
        .map(h -> h.datasetName().strip())
        .collect(toUnmodifiableSet());
  }

  private static Set<String> getEntitiesMutated(KryonDefinition kryonDefinition) {
    return kryonDefinition.allTags().getAnnotationsByType(DataAccess.class).stream()
        .filter(h -> MUTATION.equals(h.accessPattern()))
        .map(h -> h.datasetName().strip())
        .collect(toUnmodifiableSet());
  }

  CacheContainer cacheContainer() {
    return cache;
  }

  private class CachingDecoratedKryon implements Kryon<KryonCommand<?>, KryonCommandResponse> {

    private final Kryon<KryonCommand<?>, KryonCommandResponse> kryon;

    private CachingDecoratedKryon(KryonDecorationInput input) {
      this.kryon = input.kryon();
    }

    @Override
    public VajramKryonDefinition getKryonDefinition() {
      return kryon.getKryonDefinition();
    }

    @Override
    public CompletableFuture<KryonCommandResponse> executeCommand(KryonCommand<?> kryonCommand) {
      if (getKryonCachingMetadata(kryonCommand.vajramID()).isEligibleForCaching()) {
        if (kryonCommand instanceof ForwardReceiveBatch forwardBatch) {
          return readFromCache(kryon, forwardBatch);
        } else if (kryonCommand instanceof DirectForwardReceive directForwardReceive) {
          return readFromCache(kryon, directForwardReceive);
        }
      }
      // Let all other commands just pass through. Request level cache is supposed to intercept
      // only Forward Commands, and only for eligible vajrams.
      return kryon.executeCommand(kryonCommand);
    }

    private CompletableFuture<KryonCommandResponse> readFromCache(
        Kryon<KryonCommand<?>, KryonCommandResponse> kryon, DirectForwardReceive command) {
      List<ExecutionItem> cacheMisses = new ArrayList<>();
      for (ExecutionItem executionItem :
          command.executionItems(getKryonDefinition().kryonDefinitionRegistry())) {
        FacetValues facetValues = executionItem.facetValues();
        var cacheKey = newCacheKey(facetValues);
        if (cacheKey == null) {
          // Since the cache key could not be generated, we skip caching for this request
          log.error(
              "Skipping DirectForwardReceive caching for request {} since cache key is null",
              facetValues);
          cacheMisses.add(executionItem);
          continue;
        }
        // We are in a KryonDecorator. If we get cached Future which is potentially not complete,
        // and use that as the cached response hoping that it would complete later, we can introduce
        // a deadlock during graph execution.
        // This can happen if, for example, KryonDecorator for Vajram V1 is invoked with inputs I1,
        // and its response F1 is cached. That F1 might be blocked on InputBatchingDecorator being
        // flushed for all dependentChains which are part of a particular epoch lets Say E1. Later
        // Vajram V1 gets invoked with same I1, and we return F1 as is. But if this second call to
        // V1 happened via a dependentChain which is part of Epoch E0 and if some dependency chain
        // in E1 is waiting for all dependentChains of E0 to return, that would never happen in this
        // scenario because E0 is waiting for F1 and F1 is waiting for E1 to flush and some
        // dependentChain in E1 is waiting for some E0 to complete - this will cause a deadlock (We
        // encountered this in one of the applications). To prevent this deadlock, KryonDecorators,
        // or any other decorators which get invoked before the batching decorator, must never treat
        // an incomplete future as a cache hit. That why we check if an actual value is available
        // here, else we treat it as a cache miss. THe actual cache hit in this scenario will happen
        // in the caching OutputLogicDecorator (which treats incomplete futures as cache hits). The
        // output logic decorator MUST be configured to decorate AFTER the input batching decorator
        // (else the same deadlock will happen there as well).
        var cachedValue = getCachedValue(cacheKey);
        if (cachedValue == null) {
          executionItem
              .response()
              .handle(Errable::errableFrom)
              .thenAccept(value -> cache.putValue(cacheKey, value));
          cacheMisses.add(executionItem);
          continue;
        }
        cachedValue.completeFuture(executionItem.response());
      }
      return kryon.executeCommand(
          DirectForwardCommand.ofExecutionItems(
              command.vajramID(), cacheMisses, command.dependentChain()));
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private CompletableFuture<KryonCommandResponse> readFromCache(
        Kryon<KryonCommand<?>, KryonCommandResponse> kryon, ForwardReceiveBatch forwardBatch) {
      var executableRequests = forwardBatch.executableInvocations();
      Map<InvocationId, FacetValues> cacheMisses = new LinkedHashMap<>();
      Map<InvocationId, Errable<@Nullable Object>> cacheHits = new LinkedHashMap<>();
      Map<InvocationId, CompletableFuture<@Nullable Object>> newCacheEntries =
          new LinkedHashMap<>();
      executableRequests.forEach(
          (requestId, facets) -> {
            ImmutableFacetValues cacheKey = newCacheKey(facets);
            if (cacheKey == null) {
              // Since the cache key could not be generated, we skip caching for this request
              log.error(
                  "Skipping forwardBatch caching for request {} since cache key is null", facets);
              cacheMisses.put(requestId, facets);
              return;
            }
            var cachedValue = getCachedValue(cacheKey);
            if (cachedValue == null) {
              var placeHolderFuture = new CompletableFuture<@Nullable Object>();
              newCacheEntries.put(requestId, placeHolderFuture);
              placeHolderFuture
                  .handle(Errable::errableFrom)
                  .thenAccept(value -> cache.putValue(cacheKey, value));
              cacheMisses.put(requestId, cacheKey);
            } else {
              cacheHits.put(requestId, cachedValue);
            }
          });
      CompletableFuture<KryonCommandResponse> cacheMissesResponse =
          kryon.executeCommand(
              new ForwardReceiveBatch(
                  forwardBatch.vajramID(), cacheMisses, forwardBatch.dependentChain()));

      cacheMissesResponse.whenComplete(
          (kryonResponse, throwable) -> {
            if (kryonResponse instanceof BatchResponse batchResponse) {
              Map<InvocationId, Errable<Object>> responses = batchResponse.responses();
              responses.forEach(
                  (requestId, response) -> {
                    CompletableFuture<? extends @Nullable Object> future = response.toFuture();
                    CompletableFuture<@Nullable Object> destinationFuture =
                        newCacheEntries.computeIfAbsent(
                            requestId, _r -> new CompletableFuture<@Nullable Object>());
                    linkFutures(future, destinationFuture);
                  });
            } else if (throwable != null) {
              cacheMisses.forEach(
                  (requestId, response) ->
                      newCacheEntries
                          .computeIfAbsent(
                              requestId, _r -> new CompletableFuture<@Nullable Object>())
                          .completeExceptionally(wrapAsCompletionException(throwable)));
            } else {
              RuntimeException e =
                  new RuntimeException("Expecting BatchResponse. Found " + kryonResponse);
              log.error("", e);
              throw e;
            }
          });
      CompletableFuture<KryonCommandResponse> finalResponse = new CompletableFuture<>();
      Set<Entry<InvocationId, CompletableFuture<@Nullable Object>>> allFutures =
          newCacheEntries.entrySet();
      var allFuturesArray = new CompletableFuture[allFutures.size()];
      int i = 0;
      for (Entry<InvocationId, CompletableFuture<@Nullable Object>> e : allFutures) {
        allFuturesArray[i++] = e.getValue();
      }
      allOf(allFuturesArray)
          .whenComplete(
              (unused, throwable) -> {
                for (Entry<InvocationId, CompletableFuture<@Nullable Object>> entry : allFutures) {
                  cacheHits.put(
                      entry.getKey(),
                      entry.getValue().handle(Errable::errableFrom).getNow(UNKNOWN_ERROR));
                }
                finalResponse.complete(new BatchResponse(cacheHits));
              });
      return finalResponse;
    }
  }

  private static boolean isEligibleForCaching(
      KryonDefinition kryonDefinition, VajramGraph vajramGraph) {
    List<DataAccess> handlesEntities =
        kryonDefinition.allTags().getAnnotationsByType(DataAccess.class);
    if (doesMutateData(handlesEntities)) {
      // If a Vajram mutates data, then never cache it
      return false;
    }
    if (isIOVajram(kryonDefinition)) {
      // If an IO Vajram doesn't mutate any data, always cache it
      return true;
    }

    // For compute vajrams only cache if they don't read data from an entity which is mutated by
    // some other vajram in the graph.
    // This simplifies the business logic of the vajram which is mutating the entity - as that
    // vajram only needs to invalidate the cache of the IOVajram which is querying the entity and
    // not all the dependent compute vajrams
    return !doesReadMutatedEntity(handlesEntities, vajramGraph);
  }

  private static boolean doesMutateData(List<DataAccess> dataAccesses) {
    return dataAccesses.stream().map(DataAccess::accessPattern).anyMatch(MUTATION::equals);
  }

  private static boolean doesReadMutatedEntity(
      List<DataAccess> dataAccesses, VajramGraph vajramGraph) {
    if (dataAccesses.isEmpty()) {
      return false;
    }
    EntityWriters entityWriters = getEntityWriters(vajramGraph);
    for (DataAccess dataAccess : dataAccesses) {
      if (!QUERY.equals(dataAccess.accessPattern())) {
        continue;
      }
      String datasetName = dataAccess.datasetName().strip();
      if (datasetName.isBlank()) {
        // If a blank dataset is being read, it means the vajram might be reading any dataset and in
        // such cases, we always cache the vajram - because there is no safe way to know if any
        // other vajram is modifying the exact data which is being read. IO Vajrams are expected to
        // explicitly declare a dataset name for request level cache to know whether to cache the
        // vajram or not.
        continue;
      }
      if (!entityWriters.writersByEntity().getOrDefault(datasetName, List.of()).isEmpty()) {
        // This means there are vajrams which mutate the entity being read by the current vajram
        return true;
      }
    }
    return false;
  }

  private static boolean isIOVajram(KryonDefinition kryonDefinition) {
    boolean isIOVajram = false;
    Optional<OutputLogicDelegationMode> outputLogicDelegationMode =
        kryonDefinition.allTags().getAnnotationByType(OutputLogicDelegationMode.class);
    if (outputLogicDelegationMode.isPresent()
        && !ComputeDelegationMode.NONE.equals(outputLogicDelegationMode.get().value())) {
      isIOVajram = true;
    }
    return isIOVajram;
  }

  private static EntityWriters getEntityWriters(VajramGraph vajramGraph) {
    return vajramGraph.getCustomMetadata(
        EntityWriters.class,
        () -> {
          Map<String, List<VajramID>> writers = new LinkedHashMap<>();
          vajramGraph
              .kryonDefinitionRegistry()
              .getDefinitions()
              .forEach(
                  (kryonDefinition) -> {
                    List<DataAccess> dataAccesses =
                        kryonDefinition.allTags().getAnnotationsByType(DataAccess.class);
                    for (DataAccess dataAccess : dataAccesses) {
                      if (MUTATION.equals(dataAccess.accessPattern())) {
                        String datasetName = dataAccess.datasetName().strip();
                        writers
                            .computeIfAbsent(datasetName, _k -> new ArrayList<>())
                            .add(kryonDefinition.vajramID());
                      }
                    }
                  });
          return new EntityWriters(writers);
        });
  }

  /**
   * We use facets instead of request as a cache key so that some non-input facets which have been
   * specifically added (for example, injections) to act as cache keys are also taken into account.
   * This, however, is a rare use case and in most cases, all non-input facets are null at the time
   * of cache key computation and using the facets object is equivalent to using the inner request
   * object.
   *
   * @param facetValues The facet values to be used as a cache key
   */
  private static @Nullable ImmutableFacetValues newCacheKey(FacetValues facetValues) {
    ImmutableFacetValues immut;
    try {
      immut = facetValues._build();
    } catch (Exception e) {
      log.error(
          "Unable to generate cache key by 'building' facet values to create an Immutable instance as an exception was encountered while building.",
          e);
      return null;
    }
    return immut;
  }

  @Nullable CompletableFuture<@Nullable Object> getCachedFuture(ImmutableFacetValues cacheKey) {
    return cache.getFuture(cacheKey);
  }

  @Nullable Errable<Object> getCachedValue(ImmutableFacetValues cacheKey) {
    return cache.getValue(cacheKey);
  }

  void primeCache(FacetValues facetValues, CompletableFuture<@Nullable Object> data) {
    cache.putFuture(facetValues._build(), data);
  }

  private class CachingDecoratedLogic implements OutputLogic<Object> {

    private final OutputLogic<Object> logicToDecorate;
    private final OutputLogicDefinition<Object> outputLogicDefinition;

    private CachingDecoratedLogic(
        OutputLogic<Object> logicToDecorate,
        OutputLogicDefinition<Object> outputLogicDefinition,
        LogicExecutionContext logicExecutionContext) {
      this.logicToDecorate = logicToDecorate;
      this.outputLogicDefinition = outputLogicDefinition;
    }

    @Override
    public void execute(OutputLogicExecutionInput input) {
      if (!getKryonCachingMetadata(outputLogicDefinition.kryonLogicId().vajramID())
          .isEligibleForCaching()) {
        logicToDecorate.execute(input);
        return;
      }
      List<ExecutionItem> cacheMisses = new ArrayList<>();
      for (ExecutionItem executionItem : input.executionItems()) {
        FacetValues facetValues = executionItem.facetValues();
        var cacheKey = newCacheKey(facetValues);
        if (cacheKey == null) {
          // Since the cache key could not be generated, we skip caching for this request
          log.warn(
              "Skipping Output Logic caching for request {} since cache key is null", facetValues);
          cacheMisses.add(executionItem);
          continue;
        }
        var cachedFuture = getCachedFuture(cacheKey);
        if (cachedFuture == null) {
          cache.putFuture(cacheKey, executionItem.response());
          cacheMisses.add(executionItem);
          continue;
        }
        propagateCompletion(cachedFuture, executionItem.response());
      }
      logicToDecorate.execute(input.withExecutionItems(cacheMisses));
    }
  }
}
