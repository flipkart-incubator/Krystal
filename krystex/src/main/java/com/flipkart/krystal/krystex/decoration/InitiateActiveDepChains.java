package com.flipkart.krystal.krystex.decoration;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import java.util.Set;

public record InitiateActiveDepChains(VajramID vajramID, Set<DependentChain> dependantsChains)
    implements DecoratorCommand {}
