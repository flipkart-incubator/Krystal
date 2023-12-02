package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.model.DependantChain;
import com.flipkart.krystal.model.KryonId;
import com.google.common.collect.ImmutableSet;

public record ForwardGranule(
    KryonId kryonId,
    ImmutableSet<String> inputNames,
    Inputs values,
    DependantChain dependantChain,
    RequestId requestId)
    implements GranularCommand {}
