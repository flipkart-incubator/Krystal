package com.flipkart.krystal.krystex.decoration;

import com.flipkart.krystal.krystex.node.NodeDefinitionRegistry;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.node.NodeLogicId;

public record NodeLogicContext(
    NodeId nodeId, NodeLogicId nodeLogicId, NodeDefinitionRegistry nodeDefinitionRegistry) {}
