package com.flipkart.krystal.vajramexecutor.krystex.inputinjection;

import static com.flipkart.krystal.vajram.VajramID.vajramID;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.except.StackTracelessException;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardBatch;
import com.flipkart.krystal.krystex.commands.ForwardGranule;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.Kryon;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
import com.flipkart.krystal.krystex.kryon.KryonResponse;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.facets.InputDef;
import com.flipkart.krystal.vajram.facets.InputSource;
import com.flipkart.krystal.vajram.facets.VajramFacetDefinition;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
  private final @Nullable VajramInjectionProvider injectionProvider;

  InjectingDecoratedKryon(
      Kryon<KryonCommand, KryonResponse> kryon,
      VajramKryonGraph vajramKryonGraph,
      @Nullable VajramInjectionProvider injectionProvider) {
    this.kryon = kryon;
    this.vajramKryonGraph = vajramKryonGraph;
    this.injectionProvider = injectionProvider;
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
    Optional<VajramDefinition> vajramDefinition =
        vajramKryonGraph.getVajramDefinition(vajramID(kryonCommand.kryonId().value()));
    if (vajramDefinition.isPresent()
        && vajramDefinition.get().vajramMetadata().isInputInjectionNeeded()) {
      if (kryonCommand instanceof ForwardBatch forwardBatch) {
        return injectFacets(forwardBatch, vajramDefinition.get());
      } else if (kryonCommand instanceof ForwardGranule) {
        var e =
            new UnsupportedOperationException(
                "KryonInputInjector does not support KryonExecStrategy GRANULAR. Please use BATCH instead");
        log.error("", e);
        throw e;
      }
    }
    return kryon.executeCommand(kryonCommand);
  }

  private CompletableFuture<KryonResponse> injectFacets(
      ForwardBatch forwardBatch, VajramDefinition vajramDefinition) {
    Map<RequestId, Facets> requestIdToFacets = forwardBatch.executableRequests();

    ImmutableMap.Builder<RequestId, Facets> newRequests = ImmutableMap.builder();
    Set<InputDef<?>> injectableFacetDefs = new LinkedHashSet<>();
    for (VajramFacetDefinition facetDefinition :
        vajramDefinition.getVajram().getFacetDefinitions()) {
      if (facetDefinition instanceof InputDef<?> inputDef
          && inputDef.sources().contains(InputSource.SESSION)) {
        injectableFacetDefs.add(inputDef);
      }
    }

    Map<String, Errable<Object>> newInputsValues = new LinkedHashMap<>();
    for (Entry<RequestId, Facets> entry : requestIdToFacets.entrySet()) {
      RequestId requestId = entry.getKey();
      Facets facets = entry.getValue();
      Facets newFacets =
          injectFacetsOfVajram(vajramDefinition, injectableFacetDefs, facets, newInputsValues);
      newRequests.put(requestId, newFacets);
    }
    return kryon.executeCommand(
        new ForwardBatch(
            forwardBatch.kryonId(),
            newRequests.build(),
            forwardBatch.dependantChain(),
            forwardBatch.skippedRequests()));
  }

  private Facets injectFacetsOfVajram(
      VajramDefinition vajramDefinition,
      Set<InputDef<?>> injectableFacetDefs,
      Facets facets,
      Map<String, Errable<Object>> injectedValues) {
    Map<String, FacetValue<Object>> newValues = new HashMap<>();
    for (VajramFacetDefinition facetDefinition : injectableFacetDefs) {
      String inputName = facetDefinition.name();
      if (facetDefinition instanceof InputDef<?> inputDef) {
        if (facets.getInputValue(inputName).value().isPresent()) {
          continue;
        }
        // Input was not resolved by calling vajram.
        Errable<Object> value =
            injectedValues.computeIfAbsent(
                inputName, _i -> getInjectedValue(vajramDefinition.getVajram().getId(), inputDef));
        newValues.put(inputName, value);
      }
    }
    if (!newValues.isEmpty()) {
      facets.values().forEach(newValues::putIfAbsent);
      return new Facets(newValues);
    } else {
      return facets;
    }
  }

  private Errable<Object> getInjectedValue(VajramID vajramId, InputDef<?> inputDef) {
    VajramInjectionProvider inputInjector = this.injectionProvider;
    if (inputInjector == null) {
      var exception = new StackTracelessException("Dependency injector is null");
      log.error(
          "Cannot inject input {} of vajram {}",
          inputDef,
          kryon.getKryonDefinition().kryonId().value(),
          exception);
      return Errable.withError(exception);
    }
    try {
      //noinspection unchecked
      return inputInjector.get(vajramId, (InputDef<Object>) inputDef);
    } catch (Throwable e) {
      String message =
          "Could not inject input %s of vajram %s"
              .formatted(inputDef, kryon.getKryonDefinition().kryonId().value());
      log.error(message, e);
      return Errable.withError(e);
    }
  }
}
