package com.flipkart.krystal.krystex.kryondecoration;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.DependentChain;

public record KryonExecutionContext(VajramID vajramID, DependentChain dependentChain) {}
