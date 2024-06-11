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
import com.flipkart.krystal.krystex.kryon.KryonDecorator;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
import com.flipkart.krystal.krystex.kryon.KryonExecutor;
import com.flipkart.krystal.krystex.kryon.KryonId;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class KryonInputInjector implements KryonDecorator {

  public static final String DECORATOR_TYPE = KryonInputInjector.class.getName();

  @NotOnlyInitialized private final VajramKryonGraph vajramKryonGraph;

  private final @Nullable InputInjectionProvider inputInjectionProvider;

  private final Map<KryonId, DecoratedKryon> decoratedKryons = new LinkedHashMap<>();

  public KryonInputInjector(
      @UnknownInitialization VajramKryonGraph vajramKryonGraph,
      @Nullable InputInjectionProvider inputInjectionProvider) {
    this.vajramKryonGraph = vajramKryonGraph;
    this.inputInjectionProvider = inputInjectionProvider;
  }

  @Override
  public String decoratorType() {
    return DECORATOR_TYPE;
  }

  @Override
  public Kryon<KryonCommand, KryonResponse> decorateKryon(
      Kryon<KryonCommand, KryonResponse> kryon, KryonExecutor kryonExecutor) {
    return decoratedKryons.computeIfAbsent(
        kryon.getKryonDefinition().kryonId(), _k -> new DecoratedKryon(kryon));
  }

  private class DecoratedKryon implements Kryon<KryonCommand, KryonResponse> {

    private final Kryon<KryonCommand, KryonResponse> kryon;

    private DecoratedKryon(Kryon<KryonCommand, KryonResponse> kryon) {
      this.kryon = kryon;
    }

    @Override
    public void executeCommand(Flush flushCommand) {
      kryon.executeCommand(flushCommand);
    }

    @Override
    public CompletableFuture<KryonResponse> executeCommand(KryonCommand kryonCommand) {
      if (kryonCommand instanceof ForwardBatch forwardBatch) {
        return injectInputs(kryon, forwardBatch);
      } else if (kryonCommand instanceof ForwardGranule) {
        throw new UnsupportedOperationException(
            "KryonInputInjector does not support KryonExecStrategy GRANULAR. Please use BATCH instead");
      }
      return kryon.executeCommand(kryonCommand);
    }

    @Override
    public KryonDefinition getKryonDefinition() {
      return kryon.getKryonDefinition();
    }
  }

  private CompletableFuture<KryonResponse> injectInputs(
      Kryon<KryonCommand, KryonResponse> kryon, ForwardBatch forwardBatch) {
    ImmutableMap<RequestId, Facets> requestIdToFacets = forwardBatch.executableRequests();

    Set<String> newInputsNames = new LinkedHashSet<>(forwardBatch.inputNames());
    ImmutableMap.Builder<RequestId, Facets> newRequests = ImmutableMap.builder();
    requestIdToFacets.forEach(
        (requestId, facets) -> {
          Facets newFacets =
              injectFromSession(
                  vajramKryonGraph
                      .getVajramDefinition(VajramID.vajramID(forwardBatch.kryonId().value()))
                      .orElse(null),
                  facets,
                  inputInjectionProvider,
                  newInputsNames);
          newRequests.put(requestId, newFacets);
        });
    return kryon.executeCommand(
        new ForwardBatch(
            forwardBatch.kryonId(),
            ImmutableSet.copyOf(newInputsNames),
            newRequests.build(),
            forwardBatch.dependantChain(),
            forwardBatch.skippedRequests()));
  }

  static Facets injectFromSession(
      @Nullable VajramDefinition vajramDefinition,
      Facets facets,
      @Nullable InputInjectionProvider inputInjectionProvider) {
    return injectFromSession(
        vajramDefinition, facets, inputInjectionProvider, new LinkedHashSet<>());
  }

  private static Facets injectFromSession(
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
      return Errable.withError(
          new Exception("Dependency injector is null, cannot resolve SESSION input"));
    }

    if (dataType == null) {
      return Errable.withError(new Exception("Data type not found"));
    }
    Type type;
    try {
      type = dataType.javaReflectType();
    } catch (ClassNotFoundException e) {
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
