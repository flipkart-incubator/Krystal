package com.flipkart.krystal.krystex.inputinjection;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FacetValuesBuilder;
import com.flipkart.krystal.krystex.commands.DirectForwardReceive;
import com.flipkart.krystal.krystex.commands.ForwardReceiveBatch;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.BatchResponse;
import com.flipkart.krystal.krystex.kryon.Kryon;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.krystex.kryon.VajramKryonDefinition;
import com.flipkart.krystal.krystex.request.InvocationId;
import com.google.common.collect.ImmutableMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Populates a vajram's {@code INJECTION}-typed facets by calling the natively-typed {@link
 * Supplier} registered for this vajram (see {@link NativeVajramInjector}), instead of the
 * reflection-based per-facet lookup done by {@link InjectingDecoratedKryon}.
 */
final class NativeInjectingDecoratedKryon
    implements Kryon<KryonCommand<? extends KryonCommandResponse>, KryonCommandResponse> {

  private final Kryon<KryonCommand<? extends KryonCommandResponse>, KryonCommandResponse> kryon;
  private final @Nullable Function<VajramID, Supplier<FacetValuesBuilder>>
      injectedFacetsSupplierProvider;

  NativeInjectingDecoratedKryon(
      Kryon<KryonCommand<? extends KryonCommandResponse>, KryonCommandResponse> kryon,
      @Nullable Function<VajramID, Supplier<FacetValuesBuilder>> injectedFacetsSupplierProvider) {
    this.kryon = kryon;
    this.injectedFacetsSupplierProvider = injectedFacetsSupplierProvider;
  }

  @Override
  public VajramKryonDefinition getKryonDefinition() {
    return kryon.getKryonDefinition();
  }

  @Override
  public CompletableFuture<KryonCommandResponse> executeCommand(
      KryonCommand<? extends KryonCommandResponse> kryonCommand) {
    Supplier<FacetValuesBuilder> supplier =
        injectedFacetsSupplierProvider == null
            ? null
            : injectedFacetsSupplierProvider.apply(getKryonDefinition().vajramID());
    if (supplier != null) {
      if (kryonCommand instanceof ForwardReceiveBatch forwardBatch) {
        return injectFacets(forwardBatch, supplier);
      } else if (kryonCommand instanceof DirectForwardReceive forwardReceive) {
        injectFacets(forwardReceive, supplier);
      }
    }
    return kryon.executeCommand(kryonCommand);
  }

  private void injectFacets(
      DirectForwardReceive forwardReceive, Supplier<FacetValuesBuilder> supplier) {
    for (ExecutionItem executionItem :
        forwardReceive.executionItems(getKryonDefinition().kryonDefinitionRegistry())) {
      executionItem.facetValues()._mergeInjectedFacetsFrom(supplier.get());
    }
  }

  private CompletableFuture<KryonCommandResponse> injectFacets(
      ForwardReceiveBatch forwardBatch, Supplier<FacetValuesBuilder> supplier) {
    ImmutableMap.Builder<InvocationId, FacetValues> newRequests = ImmutableMap.builder();
    for (Entry<InvocationId, ? extends FacetValues> entry :
        forwardBatch.executableInvocations().entrySet()) {
      FacetValuesBuilder facetsBuilder = entry.getValue()._asBuilder();
      facetsBuilder._mergeInjectedFacetsFrom(supplier.get());
      newRequests.put(entry.getKey(), facetsBuilder);
    }
    KryonCommand<BatchResponse> newBatch =
        new ForwardReceiveBatch(
            forwardBatch.vajramID(), newRequests.build(), forwardBatch.dependentChain());
    return kryon.executeCommand(newBatch);
  }
}
