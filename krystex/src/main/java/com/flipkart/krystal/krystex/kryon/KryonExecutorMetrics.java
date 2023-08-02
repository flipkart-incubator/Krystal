package com.flipkart.krystal.krystex.kryon;

import lombok.Getter;

@Getter
public final class KryonExecutorMetrics {
  private int commandQueuedCount;
  private int commandQueueBypassedCount;

  void commandQueueBypassed() {
    this.commandQueueBypassedCount++;
  }

  void commandQueued() {
    this.commandQueuedCount++;
  }
}
