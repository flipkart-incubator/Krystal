package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.Node;
import com.flipkart.krystal.krystex.Request;

public record InitiateNode(Node<?> node, Request request) implements NodeCommand {

  public InitiateNode(Node<?> node) {
    this(node, null);
  }
}
