package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.DepResponsesImpl;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.request.RequestId;

public record CallbackGranule(
    KryonId kryonId,
    int dependencyId,
    DepResponsesImpl<?, Object> depResponses,
    RequestId requestId,
    DependantChain dependantChain)
    implements GranularCommand {}
