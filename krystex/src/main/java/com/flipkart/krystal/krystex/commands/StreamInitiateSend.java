package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.request.InvocationId;
import com.google.common.collect.ImmutableMap;

public record StreamInitiateSend(
    VajramID vajramID,
    DependentChain dependentChain,
    ImmutableMap<InvocationId, Request<?>> executableRequests)
    implements ClientSideCommand<VoidResponse> {}
