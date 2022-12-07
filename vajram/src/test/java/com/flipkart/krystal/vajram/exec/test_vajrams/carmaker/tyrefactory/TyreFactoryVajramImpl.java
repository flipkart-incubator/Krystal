package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory;

import com.flipkart.krystal.vajram.ExecutionContext;
import com.flipkart.krystal.vajram.RequestBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

// Auto generated and managed by Krystal
public class TyreFactoryVajramImpl extends TyreFactoryVajram {
    @Override
    public ImmutableList<RequestBuilder<?>> resolveInputOfDependency(
            String dependency, ImmutableSet<String> resolvableInputs, ExecutionContext executionContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<ImmutableList<Tyre>> executeBlocking(
        ExecutionContext executionContext) {

        Object requestValue = executionContext.getValue("tyre_request");
        TyreRequest tyreReqeust = null;
        if (requestValue instanceof TyreRequest.Builder builder) {
            tyreReqeust = builder.build();
        } else {
            throw new IllegalArgumentException(
                "Unsupported type %s for input %s in vajram %s"
                    .formatted(tyreReqeust.getClass(), "tyre_request", "TyreFactoryVajram"));
        }
        InputUtils.AllInputs allInputs = new InputUtils.AllInputs(new TyreRequest(
            tyreReqeust.brandName(), tyreReqeust.count(), tyreReqeust.size()));
        CompletableFuture<Tyre>  cTyre = super.makeTyre(allInputs);

        CompletableFuture<ImmutableList<Tyre>>  cIT = new CompletableFuture<>();
        cTyre.thenApply(tyre -> cIT.complete(ImmutableList.of(tyre)));

        return cIT;
    }
}
