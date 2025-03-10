package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.FlushResponse;

public record Flush(VajramID vajramID, DependantChain dependantChain)
    implements ClientSideCommand<FlushResponse>, ServerSideCommand<FlushResponse> {}
