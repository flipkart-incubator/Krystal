package com.flipkart.krystal.krystex.logicdecorators.observability;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValues;
import org.checkerframework.checker.nullness.qual.Nullable;

public record LogicExecResponse(FacetValues facetValues, Errable<@Nullable Object> response) {}
