package com.flipkart.krystal.krystex;

sealed interface NodeCommand
    permits DependencyDone, InitiateNode, NewDataFromDependency, ProvideInputValues {
  Node<?> node();
}
