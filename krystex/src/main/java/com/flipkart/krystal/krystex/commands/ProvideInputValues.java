package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.Node;
import com.flipkart.krystal.krystex.NodeInputs;

public record ProvideInputValues(Node<?> node, NodeInputs nodeInputs) implements NodeCommand {}
