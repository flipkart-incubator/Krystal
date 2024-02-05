package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.ValueOrError;
import java.util.List;
import java.util.function.Predicate;

public record SkipPredicate<T>(String reason, Predicate<List<ValueOrError<?>>> condition) {}
