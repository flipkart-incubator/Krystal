package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.krystex.resolution.ResolverCommand.SkipDependency;
import com.flipkart.krystal.model.DependantChain;
import com.flipkart.krystal.model.KryonId;

public record SkipGranule(
    KryonId kryonId,
    RequestId requestId,
    DependantChain dependantChain,
    SkipDependency skipDependencyCommand)
    implements GranularCommand {}
