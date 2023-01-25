package com.flipkart.krystal.krystex.decoration;

import com.flipkart.krystal.data.Inputs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;

public interface DecorableMainLogic<T>
    extends DecorableLogic<
        ImmutableList<Inputs>, ImmutableMap<Inputs, CompletableFuture<T>>> {}
