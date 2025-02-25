package com.flipkart.krystal.krystex.kryondecoration;

import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.core.VajramID;

public record KryonExecutionContext(VajramID vajramID, DependantChain dependantChain) {}
