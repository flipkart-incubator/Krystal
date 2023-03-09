package com.flipkart.krystal.krystex.decoration;

import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.NodeDefinitionRegistry;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.logic.LogicTag;
import com.google.common.collect.ImmutableMap;

public record LogicExecutionContext(
    NodeId nodeId,
    ImmutableMap<String, LogicTag> logicTags,
    DependantChain dependants,
    NodeDefinitionRegistry nodeDefinitionRegistry) {}
