package com.flipkart.krystal.vajramexecutor.krystex.inputinjection;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardBatch;
import com.flipkart.krystal.krystex.commands.ForwardGranule;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.Kryon;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
import com.flipkart.krystal.krystex.kryon.KryonResponse;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.facets.InputDef;
import com.flipkart.krystal.vajram.facets.InputSource;
import com.flipkart.krystal.vajram.facets.VajramFacetDefinition;
import com.flipkart.krystal.vajram.tags.AnnotationTag;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import jakarta.inject.Named;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
class InjectingDecoratedKryon implements Kryon<KryonCommand, KryonResponse> {

  private final Kryon<KryonCommand, KryonResponse> kryon;
  private final VajramKryonGraph vajramKryonGraph;
  private final @Nullable InputInjectionProvider inputInjectionProvider;

  InjectingDecoratedKryon(
      Kryon<KryonCommand, KryonResponse> kryon,
      VajramKryonGraph vajramKryonGraph,
      @Nullable InputInjectionProvider inputInjectionProvider) {
    this.kryon = kryon;
    this.vajramKryonGraph = vajramKryonGraph;
    this.inputInjectionProvider = inputInjectionProvider;
  }

  @Override
  public void executeCommand(Flush flushCommand) {
    kryon.executeCommand(flushCommand);
  }

  @Override
  public KryonDefinition getKryonDefinition() {
    return kryon.getKryonDefinition();
  }

  @Override
  public CompletableFuture<KryonResponse> executeCommand(KryonCommand kryonCommand) {
    if (kryonCommand instanceof ForwardBatch forwardBatch) {
      return injectFacets(kryon, forwardBatch);
    } else if (kryonCommand instanceof ForwardGranule) {
      var e =
          new UnsupportedOperationException(
              "KryonInputInjector does not support KryonExecStrategy GRANULAR. Please use BATCH instead");
      log.error("", e);
      throw e;
    }
    return kryon.executeCommand(kryonCommand);
  }

  private CompletableFuture<KryonResponse> injectFacets(
      Kryon<KryonCommand, KryonResponse> kryon, ForwardBatch forwardBatch) {
    ImmutableMap<RequestId, Facets> requestIdToFacets = forwardBatch.executableRequests();

    Set<String> newInputsNames = new LinkedHashSet<>(forwardBatch.inputNames());
    ImmutableMap.Builder<RequestId, Facets> newRequests = ImmutableMap.builder();
    for (Entry<RequestId, Facets> entry : requestIdToFacets.entrySet()) {
      RequestId requestId = entry.getKey();
      Facets facets = entry.getValue();
      Facets newFacets =
          injectFacetsOfVajram(
              vajramKryonGraph
                  .getVajramDefinition(VajramID.vajramID(forwardBatch.kryonId().value()))
                  .orElse(null),
              facets,
              inputInjectionProvider,
              newInputsNames);
      newRequests.put(requestId, newFacets);
    }
    return kryon.executeCommand(
        new ForwardBatch(
            forwardBatch.kryonId(),
            ImmutableSet.copyOf(newInputsNames),
            newRequests.build(),
            forwardBatch.dependantChain(),
            forwardBatch.skippedRequests()));
  }

  private static Facets injectFacetsOfVajram(
      @Nullable VajramDefinition vajramDefinition,
      Facets facets,
      @Nullable InputInjectionProvider inputInjectionProvider,
      Set<String> newInputNames) {
    Map<String, FacetValue<Object>> newValues = new HashMap<>();
    ImmutableMap<String, ImmutableMap<Object, Tag>> facetTags =
        vajramDefinition == null ? ImmutableMap.of() : vajramDefinition.getFacetTags();
    Optional.ofNullable(vajramDefinition)
        .map(VajramDefinition::getVajram)
        .map(Vajram::getFacetDefinitions)
        .ifPresent(
            facetDefinitions -> {
              for (VajramFacetDefinition facetDefinition : facetDefinitions) {
                String inputName = facetDefinition.name();
                if (facetDefinition instanceof InputDef<?> inputDef) {
                  if (inputDef.sources().contains(InputSource.CLIENT)) {
                    Errable<Object> value = facets.getInputValue(inputName);
                    if (!Errable.empty().equals(value)) {
                      continue;
                    }
                  }
                  // Input was not resolved by calling vajram.
                  // Check if it is resolvable by SESSION
                  if (inputDef.sources().contains(InputSource.SESSION)) {
                    ImmutableMap<Object, Tag> inputTags =
                        facetTags.getOrDefault(inputName, ImmutableMap.of());
                    Errable<Object> value =
                        getFromInjectionAdaptor(
                            inputDef.type(),
                            Optional.ofNullable(inputTags.get(Named.class))
                                .map(
                                    tag -> {
                                      if (tag instanceof AnnotationTag<?> annoTag
                                          && annoTag.tagValue() instanceof Named named) {
                                        return named.value();
                                      }
                                      return null;
                                    })
                                .orElse(null),
                            inputInjectionProvider);
                    newValues.put(inputName, value);
                    newInputNames.add(inputName);
                  }
                }
              }
            });
    if (!newValues.isEmpty()) {
      facets.values().forEach(newValues::putIfAbsent);
      return new Facets(newValues);
    } else {
      return facets;
    }
  }

  private static Errable<Object> getFromInjectionAdaptor(
      DataType<?> dataType,
      @Nullable String injectionName,
      @Nullable InputInjectionProvider inputInjectionProvider) {
    if (inputInjectionProvider == null) {
      var exception =
          new RuntimeException("Dependency injector is null, cannot resolve SESSION input");
      log.error("", exception);
      return Errable.withError(exception);
    }

    if (dataType == null) {
      var exception = new RuntimeException("Data type not found");
      log.error("", exception);
      return Errable.withError(exception);
    }
    Type type;
    try {
      type = dataType.javaReflectType();
    } catch (ClassNotFoundException e) {
      log.error("", e);
      return Errable.withError(e);
    }
    @Nullable Object resolvedObject = null;
    if (injectionName != null) {
      resolvedObject = inputInjectionProvider.getInstance((Class<?>) type, injectionName);
    }
    if (resolvedObject == null) {
      resolvedObject = inputInjectionProvider.getInstance(((Class<?>) type));
    }
    return Errable.withValue(resolvedObject);
  }
}
