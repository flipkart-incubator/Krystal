package com.flipkart.krystal.krystex.logicdecoration;

import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonId;
import java.util.Set;

public record InitiateActiveDepChains(KryonId kryonId, Set<DependantChain> dependantsChains)
    implements LogicDecoratorCommand {}
