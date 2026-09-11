package com.flipkart.krystal.krystex.logicdecoration;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.tags.ElementTags;

public record LogicDecorationContext(
    VajramID vajramID, ElementTags logicTags, KryonDefinitionRegistry kryonDefinitionRegistry) {}
