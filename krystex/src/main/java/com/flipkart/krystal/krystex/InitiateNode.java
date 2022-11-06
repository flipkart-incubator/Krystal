package com.flipkart.krystal.krystex;

public record InitiateNode(Node<?> node, Request request) implements NodeCommand {

  public InitiateNode(Node<?> node) {
    this(node, null);
  }
}
