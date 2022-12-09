package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.Node;

public sealed interface NodeCommand
    permits DependencyDone, InitiateNode, NewDataFromDependency, ProvideInputValues {
  Node<?> node();
}
