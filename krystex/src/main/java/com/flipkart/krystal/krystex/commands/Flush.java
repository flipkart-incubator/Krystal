package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.DependentChain;

public record Flush(VajramID vajramID, DependentChain dependentChain)
    implements KryonCommand<VoidResponse>, ClientSideCommand<VoidResponse> {}
