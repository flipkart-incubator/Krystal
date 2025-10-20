package com.flipkart.krystal.krystex.logicdecoration;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.tags.ElementTags;

public record LogicExecutionContext(
    VajramID vajramID,
    ElementTags logicTags,
    DependentChain dependents,
    KryonDefinitionRegistry kryonDefinitionRegistry) {}
