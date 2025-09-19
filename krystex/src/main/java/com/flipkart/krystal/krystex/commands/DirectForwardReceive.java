package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.DirectResponse;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public record DirectForwardReceive(
    VajramID vajramID, List<ExecutionItem> executableRequests, DependentChain dependentChain)
    implements MultiRequestDirectCommand<DirectResponse>, ServerSideCommand<DirectResponse> {}
