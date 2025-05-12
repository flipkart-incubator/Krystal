package com.flipkart.krystal.krystex.decoration;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.google.common.collect.ImmutableSet;

public record InitiateActiveDepChains(
    VajramID vajramID, ImmutableSet<DependentChain> dependantsChains) implements DecoratorCommand {}
