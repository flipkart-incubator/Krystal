package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.SingleValue;

public record ExecuteWithInput(
    NodeId nodeId, String input, SingleValue<?> inputValue, RequestId requestId)
    implements NodeCommand {}
