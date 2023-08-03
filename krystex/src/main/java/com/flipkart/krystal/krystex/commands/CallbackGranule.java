package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.request.RequestId;

public record CallbackGranule(
    KryonId kryonId,
    String dependencyName,
    Results<Object> results,
    RequestId requestId,
    DependantChain dependantChain)
    implements GranularCommand {}
