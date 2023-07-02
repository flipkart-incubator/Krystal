package com.flipkart.krystal.krystex.node;

import java.time.Duration;

@SuppressWarnings("PackageVisibleField")
public final class KrystalNodeExecutorMetrics {

  long totalNodeTimeNs;
  long computeNodeCommandsTimeNs;
  long propagateNodeCommandsNs;
  long executeMainLogicTimeNs;
  long registerDepCallbacksTimeNs;
  long flushDependencyIfNeededTimeNs;
  long decorateMainLogicTimeNs;

  long commandQueuedCount;
  long commandQueueBypassedCount;
  long nodeInputsBatchCount;
  long depCallbackBatchCount;
  long executeMainLogicCount;

  public void add(KrystalNodeExecutorMetrics other) {
    totalNodeTimeNs += other.totalNodeTimeNs;
    computeNodeCommandsTimeNs += other.computeNodeCommandsTimeNs;
    propagateNodeCommandsNs += other.propagateNodeCommandsNs;
    executeMainLogicTimeNs += other.executeMainLogicTimeNs;
    registerDepCallbacksTimeNs += other.registerDepCallbacksTimeNs;
    flushDependencyIfNeededTimeNs += other.flushDependencyIfNeededTimeNs;
    decorateMainLogicTimeNs += other.decorateMainLogicTimeNs;

    commandQueuedCount += other.commandQueuedCount;
    commandQueueBypassedCount += other.commandQueueBypassedCount;
    nodeInputsBatchCount += other.nodeInputsBatchCount;
    depCallbackBatchCount += other.depCallbackBatchCount;
    executeMainLogicCount += other.executeMainLogicCount;
  }

  @Override
  public String toString() {
    return """
              KrystalNodeExecutorMetrics{
                totalNodeTimeNs                 %s
                computeNodeCommandsTime         %s
                propagateNodeCommands           %s
                executeMainLogicTime            %s
                registerDepCallbacksTime        %s
                flushDependencyIfNeededTime     %s
                decorateMainLogicTime           %s

                commandQueuedCount              %,d
                commandQueueBypassedCount       %,d
                nodeInputsBatchCount            %,d
                depCallbackBatchCount           %,d
                executeMainLogicCount           %,d
              }
              """
        .formatted(
            toString(Duration.ofNanos(totalNodeTimeNs)),
            toString(Duration.ofNanos(computeNodeCommandsTimeNs)),
            toString(Duration.ofNanos(propagateNodeCommandsNs)),
            toString(Duration.ofNanos(executeMainLogicTimeNs)),
            toString(Duration.ofNanos(registerDepCallbacksTimeNs)),
            toString(Duration.ofNanos(flushDependencyIfNeededTimeNs)),
            toString(Duration.ofNanos(decorateMainLogicTimeNs)),
            commandQueuedCount,
            commandQueueBypassedCount,
            nodeInputsBatchCount,
            depCallbackBatchCount,
            executeMainLogicCount);
  }

  private static String toString(Duration duration) {
    return "%dm %ds %dns"
        .formatted(duration.toMinutes(), duration.toSecondsPart(), duration.toNanosPart());
  }
}
