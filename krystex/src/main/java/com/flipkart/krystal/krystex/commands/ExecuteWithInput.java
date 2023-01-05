package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.Value;
import com.flipkart.krystal.krystex.node.NodeId;

public record ExecuteWithInput(NodeId nodeId, String input, Value inputValue, RequestId requestId)
    implements NodeCommand {}
