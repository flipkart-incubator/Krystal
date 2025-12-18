package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.DirectResponse;
import java.util.List;

public record DirectForwardReceive(
    VajramID vajramID, List<ExecutionItem> executionItems, DependentChain dependentChain)
    implements MultiRequestDirectCommand, ServerSideCommand<DirectResponse> {}
