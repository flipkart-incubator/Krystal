package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.krystex.MultiResultFuture;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.function.Function;

public interface NodeLogic<T>
    extends Function<ImmutableList<NodeInputs>, ImmutableMap<NodeInputs, MultiResultFuture<T>>> {}
