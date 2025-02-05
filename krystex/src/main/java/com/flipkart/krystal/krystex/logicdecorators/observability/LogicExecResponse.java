package com.flipkart.krystal.krystex.logicdecorators.observability;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValues;

public record LogicExecResponse(FacetValues facetValues, Errable<Object> response) {}
