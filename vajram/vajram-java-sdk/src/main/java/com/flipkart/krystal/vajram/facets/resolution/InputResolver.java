package com.flipkart.krystal.vajram.facets.resolution;

public sealed interface InputResolver extends InputResolverDefinition
    permits AbstractInputResolver, FanoutInputResolver, SimpleInputResolver, SingleInputResolver {}
