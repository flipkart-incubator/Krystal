package com.flipkart.krystal.krystex.logicdecoration;

import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.core.VajramID;
import com.google.common.collect.ImmutableSet;

public record InitiateActiveDepChains(
    VajramID vajramID, ImmutableSet<DependantChain> dependantsChains)
    implements LogicDecoratorCommand {}
