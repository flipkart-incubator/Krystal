package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.krystex.resolution.ResolverCommand.SkipDependency;

public record SkipGranule(
    KryonId kryonId,
    RequestId requestId,
    DependantChain dependantChain,
    SkipDependency skipDependencyCommand)
    implements GranularCommand {}
