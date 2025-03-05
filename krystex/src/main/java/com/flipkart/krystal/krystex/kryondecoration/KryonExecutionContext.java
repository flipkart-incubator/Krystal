package com.flipkart.krystal.krystex.kryondecoration;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.DependantChain;

public record KryonExecutionContext(VajramID vajramID, DependantChain dependantChain) {}
