package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.request.RequestId;

public sealed interface NodeRequestCommand extends NodeCommand
    permits ExecuteWithDependency, ExecuteWithInputs, SkipNode {

  RequestId requestId();
}
