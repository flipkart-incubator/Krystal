package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.node.NodeId;

public record ExecuteWithInput(
    NodeId nodeId, String name, InputValue<?> inputValue, RequestId requestId)
    implements NodeCommand {}
