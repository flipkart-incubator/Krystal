package com.flipkart.krystal.krystex.logicdecoration;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.google.common.collect.ImmutableMap;

public record LogicExecutionContext(
    KryonId kryonId,
    ImmutableMap<Object, Tag> logicTags,
    DependantChain dependants,
    KryonDefinitionRegistry kryonDefinitionRegistry) {}
