package com.flipkart.krystal.vajramexecutor.krystex.inputinjection;

import static com.flipkart.krystal.core.VajramID.vajramID;
import static com.flipkart.krystal.data.Errable.errableFrom;
import static com.flipkart.krystal.facets.FacetType.INJECTION;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.ErrableFacetValue;
import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FacetValuesBuilder;
import com.flipkart.krystal.data.Failure;
import com.flipkart.krystal.except.StackTracelessException;
import com.flipkart.krystal.krystex.commands.DirectForwardReceive;
import com.flipkart.krystal.krystex.commands.ForwardReceiveBatch;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.BatchResponse;
import com.flipkart.krystal.krystex.kryon.Kryon;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.krystex.kryon.VajramKryonDefinition;
import com.flipkart.krystal.krystex.request.InvocationId;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.facets.specs.DefaultFacetSpec;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
import com.google.common.collect.ImmutableMap;
import jakarta.inject.Provider;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
class InjectingDecoratedKryon
    implements Kryon<KryonCommand<? extends KryonCommandResponse>, KryonCommandResponse> {

  private final Kryon<KryonCommand<? extends KryonCommandResponse>, KryonCommandResponse> kryon;
  private final VajramGraph vajramGraph;
  private final @Nullable VajramInjectionProvider injectionProvider;

  InjectingDecoratedKryon(
      Kryon<KryonCommand<? extends KryonCommandResponse>, KryonCommandResponse> kryon,
      VajramGraph vajramGraph,
      @Nullable VajramInjectionProvider injectionProvider) {
    this.kryon = kryon;
    this.vajramGraph = vajramGraph;
    this.injectionProvider = injectionProvider;
  }

  @Override
  public VajramKryonDefinition getKryonDefinition() {
    return kryon.getKryonDefinition();
  }

  @Override
  public CompletableFuture<KryonCommandResponse> executeCommand(
      KryonCommand<? extends KryonCommandResponse> kryonCommand) {
    VajramDefinition vajramDefinition =
        vajramGraph.getVajramDefinition(vajramID(kryonCommand.vajramID().id()));
    if (vajramDefinition.metadata().isInputInjectionNeeded()
        && vajramDefinition.def() instanceof VajramDef<?>) {
      if (kryonCommand instanceof ForwardReceiveBatch forwardBatch) {
        return injectFacets(forwardBatch, vajramDefinition);
      } else if (kryonCommand instanceof DirectForwardReceive forwardReceive) {
        return injectFacets(forwardReceive, vajramDefinition);
      }
    }
    return kryon.executeCommand(kryonCommand);
  }

  private CompletableFuture<KryonCommandResponse> injectFacets(
      DirectForwardReceive forwardReceive, VajramDefinition vajramDefinition) {

    Set<FacetSpec<?, ?>> injectableFacets = new LinkedHashSet<>();
    vajramDefinition
        .facetSpecs()
        .forEach(
            facetSpec -> {
              if (INJECTION.equals(facetSpec.facetType())) {
                injectableFacets.add(facetSpec);
              }
            });

    for (ExecutionItem executionItem : forwardReceive.executionItems()) {
      FacetValuesBuilder facetsBuilder = executionItem.facetValues();
      List<AutoCloseable> closeables =
          injectFacetsOfVajram(vajramDefinition, injectableFacets, facetsBuilder);
      // For DI frameworks which support manual lifecycle management, call close so that any
      // instance with custom lifecycle can be destroyed. Here we call close when the response is
      // received.
      executionItem
          .response()
          .whenComplete(
              (o, throwable) -> {
                for (var closeable : closeables) {
                  try {
                    closeable.close();
                  } catch (Exception e) {
                    log.error("Failed to close injected closeable", e);
                  }
                }
              });
    }
    return kryon.executeCommand(forwardReceive);
  }

  private CompletableFuture<KryonCommandResponse> injectFacets(
      ForwardReceiveBatch forwardBatch, VajramDefinition vajramDefinition) {
    Map<InvocationId, ? extends FacetValues> requestIdToFacets =
        forwardBatch.executableInvocations();

    ImmutableMap.Builder<InvocationId, FacetValues> newRequests = ImmutableMap.builder();
    Set<FacetSpec<?, ?>> injectableFacets = new LinkedHashSet<>();
    vajramDefinition
        .facetSpecs()
        .forEach(
            facetSpec -> {
              if (INJECTION.equals(facetSpec.facetType())) {
                injectableFacets.add(facetSpec);
              }
            });

    for (Entry<InvocationId, ? extends FacetValues> entry : requestIdToFacets.entrySet()) {
      InvocationId invocationId = entry.getKey();
      FacetValuesBuilder facetsBuilder;
      facetsBuilder = entry.getValue()._asBuilder();
      injectFacetsOfVajram(vajramDefinition, injectableFacets, facetsBuilder);
      newRequests.put(invocationId, facetsBuilder);
    }
    KryonCommand<BatchResponse> kryonCommand =
        new ForwardReceiveBatch(
            forwardBatch.vajramID(),
            newRequests.build(),
            forwardBatch.dependentChain(),
            forwardBatch.invocationsToSkip());
    CompletableFuture<KryonCommandResponse> resp = kryon.executeCommand(kryonCommand);
    resp.whenComplete((kryonCommandResponse, throwable) -> {});
    return resp;
  }

  private List<AutoCloseable> injectFacetsOfVajram(
      VajramDefinition vajramDefinition,
      Set<FacetSpec<?, ?>> injectableFacets,
      FacetValuesBuilder facetsBuilder) {
    List<AutoCloseable> closeables = new ArrayList<>();
    for (var facetSpec : injectableFacets) {
      if (!(facetSpec instanceof DefaultFacetSpec<?, ?> defaultFacetSpec)) {
        continue;
      }
      Errable<?> facetValue = defaultFacetSpec.getFacetValue(facetsBuilder).asErrable();
      if (facetValue.valueOpt().isPresent()) {
        continue;
      }
      Provider<Object> provider;
      Errable<Object> value;
      try {
        provider = getInjectedValue(vajramDefinition.vajramId(), facetSpec);
        if (provider instanceof AutoCloseable closeable) {
          closeables.add(closeable);
        }
        value = errableFrom(provider::get);
      } catch (Exception e) {
        log.error(
            "Could not inject input {} of vajram {}",
            facetSpec,
            kryon.getKryonDefinition().vajramID().id(),
            e);
        value = Errable.withError(e);
      }
      if (value instanceof Failure<Object> f) {
        defaultFacetSpec.setFacetValue(facetsBuilder, new ErrableFacetValue<>(f));
        log.error(
            "Could not inject input {} of vajram {}",
            facetSpec,
            kryon.getKryonDefinition().vajramID().id(),
            f.error());
      }
      defaultFacetSpec.setFacetValue(facetsBuilder, new ErrableFacetValue<>(value));
    }
    return closeables;
  }

  @SuppressWarnings("unchecked")
  private Provider<Object> getInjectedValue(VajramID vajramId, FacetSpec<?, ?> facetDef)
      throws Exception {
    VajramInjectionProvider inputInjector = this.injectionProvider;
    if (inputInjector == null) {
      throw new StackTracelessException("Dependency injector is null");
    }
    return (Provider<Object>) inputInjector.get(vajramId, facetDef);
  }
}
