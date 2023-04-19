package com.flipkart.krystal.krystex.node;

public record NodeLogicId(NodeId nodeId, String value) {

  @Override
  public String toString() {
    return "l<%s>".formatted(value());
  }
}
