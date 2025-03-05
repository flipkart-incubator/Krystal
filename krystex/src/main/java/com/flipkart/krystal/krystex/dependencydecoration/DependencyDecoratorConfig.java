package com.flipkart.krystal.krystex.dependencydecoration;

import java.util.function.Function;
import java.util.function.Predicate;

public record DependencyDecoratorConfig(
    String decoratorType,
    Predicate<DependencyExecutionContext> shouldDecorate,
    Function<DependencyExecutionContext, String> instanceIdGenerator,
    Function<DependencyExecutionContext, DependencyDecorator> factory) {}
