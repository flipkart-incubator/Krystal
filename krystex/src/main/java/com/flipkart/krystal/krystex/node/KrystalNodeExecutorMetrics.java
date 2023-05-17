package com.flipkart.krystal.krystex.node;

import lombok.Getter;

@Getter
public final class KrystalNodeExecutorMetrics {
  private int commandQueuedCount;
  private int commandQueueBypassedCount;

  void commandQueueBypassed() {
    this.commandQueueBypassedCount++;
  }

  void commandQueued() {
    this.commandQueuedCount++;
  }
}
