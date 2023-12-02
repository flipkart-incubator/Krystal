package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.model.DependantChain;
import com.flipkart.krystal.model.KryonId;

public record CallbackGranule(
    KryonId kryonId,
    String dependencyName,
    Results<Object> results,
    RequestId requestId,
    DependantChain dependantChain)
    implements GranularCommand {}
