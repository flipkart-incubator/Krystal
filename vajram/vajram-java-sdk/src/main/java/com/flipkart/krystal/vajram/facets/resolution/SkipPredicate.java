package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import java.util.List;
import java.util.function.Predicate;

public record SkipPredicate(String reason, Predicate<FacetValue<?>> condition) {}
