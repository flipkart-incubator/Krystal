package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.FacetValue;
import java.util.function.Predicate;

public record SkipPredicate(String reason, Predicate<FacetValue<?>> condition) {}
