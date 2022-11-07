package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.Node;

sealed public interface NodeCommand
    permits DependencyDone, InitiateNode, NewDataFromDependency, ProvideInputValues {
  Node<?> node();
}
