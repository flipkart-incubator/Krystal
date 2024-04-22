package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableSet;

public record ForwardGranule(
    KryonId kryonId,
    ImmutableSet<Integer> inputIds,
    Request<Object> request,
    DependantChain dependantChain,
    RequestId requestId)
    implements GranularCommand {}
