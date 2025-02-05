package com.flipkart.krystal.krystex.logicdecorators.observability;

import com.google.common.collect.ImmutableList;

public record LogicExecResults(ImmutableList<LogicExecResponse> responses) {}
