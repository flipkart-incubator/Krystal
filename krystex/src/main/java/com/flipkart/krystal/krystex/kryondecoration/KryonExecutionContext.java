package com.flipkart.krystal.krystex.kryondecoration;

import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonId;

public record KryonExecutionContext(KryonId kryonId, DependantChain dependantChain) {}
