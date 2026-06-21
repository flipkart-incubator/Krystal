package com.flipkart.krystal.visualization.executiongraph;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValues;
import org.checkerframework.checker.nullness.qual.Nullable;

public record LogicExecResponse(
    FacetValues facetValues, Errable<? extends @Nullable Object> response) {}
