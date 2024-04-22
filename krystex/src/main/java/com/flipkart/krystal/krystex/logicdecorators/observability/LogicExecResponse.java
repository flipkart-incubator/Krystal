package com.flipkart.krystal.krystex.logicdecorators.observability;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Facets;

public record LogicExecResponse(Facets request, Errable<Object> response) {}
