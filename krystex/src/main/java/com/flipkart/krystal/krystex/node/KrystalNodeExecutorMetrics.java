package com.flipkart.krystal.krystex.node;

import java.time.Duration;

@SuppressWarnings("PackageVisibleField")
public final class KrystalNodeExecutorMetrics {

  private final NodeMetrics allNodeMetrics;

  long commandQueuedCount;
  long commandQueueBypassedCount;

  public KrystalNodeExecutorMetrics(NodeMetrics allNodeMetrics) {
    this.allNodeMetrics = allNodeMetrics;
  }

  public void add(KrystalNodeExecutorMetrics other) {
    commandQueuedCount += other.commandQueuedCount;
    commandQueueBypassedCount += other.commandQueueBypassedCount;
    this.allNodeMetrics.add(other.allNodeMetrics);
  }

  @Override
  public String toString() {
    return """
              KrystalNodeExecutorMetrics{
                commandQueuedCount              %,d
                commandQueueBypassedCount       %,d

                allNodeMetrics:
                %s
              }
              """
        .formatted(commandQueuedCount, commandQueueBypassedCount, allNodeMetrics);
  }

  private static String toString(Duration duration) {
    return "%dm %ds %dns"
        .formatted(duration.toMinutes(), duration.toSecondsPart(), duration.toNanosPart());
  }
}
