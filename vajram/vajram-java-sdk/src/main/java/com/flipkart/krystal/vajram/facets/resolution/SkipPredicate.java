package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Errable;
import java.util.List;
import java.util.function.Predicate;

public record SkipPredicate<T>(String reason, Predicate<List<Errable<?>>> condition) {}
