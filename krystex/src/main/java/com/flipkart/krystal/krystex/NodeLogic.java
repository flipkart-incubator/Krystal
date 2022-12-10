package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.function.Function;

public interface NodeLogic<T>
    extends Function<ImmutableList<NodeInputs>, ImmutableMap<NodeInputs, MultiResult<T>>> {}
