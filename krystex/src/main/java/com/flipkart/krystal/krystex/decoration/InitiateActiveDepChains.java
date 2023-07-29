package com.flipkart.krystal.krystex.decoration;

import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.google.common.collect.ImmutableSet;

public record InitiateActiveDepChains(
    KryonId kryonId, ImmutableSet<DependantChain> dependantsChains)
    implements LogicDecoratorCommand {}
