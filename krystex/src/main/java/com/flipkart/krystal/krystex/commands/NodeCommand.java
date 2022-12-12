package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.Node;

public sealed interface NodeCommand
    permits ProvideInputValues, InitiateNode, NewDataFromDependency, DependencyNodeDone, DependencyInputDone {
  Node<?> node();
}
