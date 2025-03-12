package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.FlushResponse;

public record Flush(VajramID vajramID, DependentChain dependentChain)
    implements ClientSideCommand<FlushResponse>, ServerSideCommand<FlushResponse> {}
