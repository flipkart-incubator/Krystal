package com.flipkart.krystal.vajram.inputs.resolution.internal;

import java.util.Optional;
import java.util.function.Predicate;

public record SkipPredicate<T>(String reason, Predicate<Optional<T>> condition) {}
