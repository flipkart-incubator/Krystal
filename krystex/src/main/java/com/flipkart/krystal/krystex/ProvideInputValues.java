package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableMap;

public record ProvideInputValues(Node<?> node, ImmutableMap<String, Object> values)
    implements NodeCommand {}
