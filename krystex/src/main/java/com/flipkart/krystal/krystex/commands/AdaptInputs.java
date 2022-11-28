package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.Node;
import com.google.common.collect.ImmutableMap;

public record AdaptInputs(Node<?> node, ImmutableMap<String, Object> values) implements NodeCommand {}