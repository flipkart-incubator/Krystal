package com.flipkart.krystal.krystex.logicdecoration;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.tags.ElementTags;
import java.util.Set;
import java.util.function.Supplier;

public record LogicDecorationContext(
    VajramID vajramID,
    ElementTags logicTags,
    KryonDefinitionRegistry kryonDefinitionRegistry,
    Supplier<Set<DependentChain>> activeDependentChains) {}
