package com.flipkart.krystal.krystex.decoration;

import com.flipkart.krystal.model.DependantChain;
import com.flipkart.krystal.model.KryonId;
import com.google.common.collect.ImmutableSet;

public record InitiateActiveDepChains(
    KryonId kryonId, ImmutableSet<DependantChain> dependantsChains)
    implements LogicDecoratorCommand {}
