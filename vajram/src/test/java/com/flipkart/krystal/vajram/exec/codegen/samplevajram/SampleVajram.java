package com.flipkart.krystal.vajram.exec.codegen.samplevajram;

import com.flipkart.krystal.vajram.BlockingVajram;
import com.flipkart.krystal.vajram.ExecutionContext;
import com.flipkart.krystal.vajram.NonBlockingVajram;
import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SampleVajram extends NonBlockingVajram<String> {
    @Override
    public List<VajramInputDefinition> getInputDefinitions() {
        return null;
    }

    @Override
    public ImmutableList<String> executeNonBlocking(ExecutionContext executionContext) {
        return null;
    }

    @Override
    public ImmutableList<RequestBuilder<?>> resolveInputOfDependency(String dependency, ImmutableSet resolvableInputs, ExecutionContext executionContext) {
        return null;
    }
}
