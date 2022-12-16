package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.krystex.SingleValue;
import com.google.common.collect.ImmutableMap;

public record NodeLogicExecutorInputs(ImmutableMap<String, SingleValue<?>> values) {}
