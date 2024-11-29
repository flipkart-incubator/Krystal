package com.flipkart.krystal.krystex.logicdecoration;

import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.tags.ElementTags;

public record LogicExecutionContext(
    KryonId kryonId,
    ElementTags logicTags,
    DependantChain dependants,
    KryonDefinitionRegistry kryonDefinitionRegistry) {}
