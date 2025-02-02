package com.flipkart.krystal.vajramexecutor.krystex.inputinjection;

import static com.flipkart.krystal.facets.FacetType.INJECTION;
import static com.flipkart.krystal.vajram.VajramID.vajramID;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetContainer;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.FacetsBuilder;
import com.flipkart.krystal.data.Failure;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.Success;
import com.flipkart.krystal.except.StackTracelessException;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardReceive;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.Kryon;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
import com.flipkart.krystal.krystex.kryon.KryonResponse;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.facets.specs.DefaultFacetSpec;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashSet;
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
      if (kryonCommand instanceof ForwardReceive forwardBatch) {
        return injectFacets(forwardBatch, vajramDefinition.get());
      }
    }
    return kryon.executeCommand(kryonCommand);
  }

  private CompletableFuture<KryonResponse> injectFacets(
      ForwardReceive forwardBatch, VajramDefinition vajramDefinition) {
    ImmutableMap<RequestId, ? extends FacetContainer> requestIdToFacets =
        forwardBatch.executableRequests();

    ImmutableMap.Builder<RequestId, FacetsBuilder> newRequests = ImmutableMap.builder();
    Set<FacetSpec<?, ?>> injectableFacets = new LinkedHashSet<>();
    vajramDefinition
        .facetSpecs()
        .forEach(
            (facetSpec) -> {
              if (facetSpec.facetTypes().contains(INJECTION)) {
                injectableFacets.add(facetSpec);
              }
            });

    for (Entry<RequestId, ? extends FacetContainer> entry : requestIdToFacets.entrySet()) {
      RequestId requestId = entry.getKey();
      FacetContainer container = entry.getValue();
      FacetsBuilder facetsBuilder;
      if (container instanceof Request request) {
        facetsBuilder = vajramDefinition.vajram().facetsFromRequest(request);
      } else if (container instanceof Facets facets) {
        facetsBuilder = facets._asBuilder();
      } else {
        throw new UnsupportedOperationException(
            "Unknown facet container type " + container.getClass());
      }
      newRequests.put(
          requestId, injectFacetsOfVajram(vajramDefinition, injectableFacets, facetsBuilder));
    }
    return kryon.executeCommand(
        new ForwardReceive(
            forwardBatch.kryonId(),
            newRequests.build(),
            forwardBatch.dependantChain(),
            forwardBatch.skippedRequests()));
  }

  private FacetsBuilder injectFacetsOfVajram(
      VajramDefinition vajramDefinition,
      Set<FacetSpec<?, ?>> injectableFacets,
      FacetsBuilder facetsBuilder) {
    for (FacetSpec facetSpec : injectableFacets) {
      if (!(facetSpec instanceof DefaultFacetSpec defaultFacetSpec)) {
        continue;
      }
      Errable<?> facetValue = defaultFacetSpec.getFacetValue(facetsBuilder);
      if (facetValue.valueOpt().isPresent()) {
        continue;
      }
      // Input was not resolved by calling vajram.
      Errable<Object> injectedValue = getInjectedValue(vajramDefinition.vajramId(), facetSpec);
      if (injectedValue instanceof Success<Object> success) {
        defaultFacetSpec.setFacetValue(facetsBuilder, success);
      } else if (injectedValue instanceof Failure<Object> f) {
        log.error(
            "Could not inject input {} of vajram {}",
            facetSpec,
            kryon.getKryonDefinition().kryonId().value(),
            f.error());
      }
    }
    return facetsBuilder;
  }

  private Errable<Object> getInjectedValue(VajramID vajramId, FacetSpec facetDef) {
    VajramInjectionProvider inputInjector = this.injectionProvider;
    if (inputInjector == null) {
      var exception = new StackTracelessException("Dependency injector is null");
      log.error(
          "Cannot inject input {} of vajram {}",
          facetDef,
          kryon.getKryonDefinition().kryonId().value(),
          exception);
      return Errable.withError(exception);
    }
    try {
      return (Errable<Object>) inputInjector.get(vajramId, facetDef);
    } catch (Throwable e) {
      String message =
          "Could not inject input %s of vajram %s"
              .formatted(facetDef, kryon.getKryonDefinition().kryonId().value());
      log.error(message, e);
      return Errable.withError(e);
    }
  }
}
