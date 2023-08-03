package com.flipkart.krystal.vajram.inputs.resolution;

import java.util.Optional;
import java.util.function.Predicate;

record SkipPredicate<T>(String reason, Predicate<Optional<T>> condition) {}
