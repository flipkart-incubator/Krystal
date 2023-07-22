package com.flipkart.krystal.krystex.decoration;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.google.common.collect.ImmutableMap;

public record LogicExecutionContext(
    KryonId kryonId,
    ImmutableMap<String, Tag> logicTags,
    DependantChain dependants,
    KryonDefinitionRegistry kryonDefinitionRegistry) {}
