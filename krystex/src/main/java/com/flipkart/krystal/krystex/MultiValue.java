package com.flipkart.krystal.krystex;

import com.flipkart.krystal.krystex.node.NodeInputs;
import com.google.common.collect.ImmutableMap;

public record MultiValue<T>(ImmutableMap<NodeInputs, SingleValue<T>> values) implements Value {}
