package com.flipkart.krystal.krystex.node;

import java.time.Duration;

@SuppressWarnings("PackageVisibleField")
public final class KrystalNodeExecutorMetrics {

  long commandQueuedCount;
  long commandQueueBypassedCount;
  long nodeInputsBatchCount;
  long depCallbackBatchCount;
  long executeMainLogicCount;
  long computeNodeCommandsTimeNs;
  long propagateNodeCommandsNs;
  long executeMainLogicTimeNs;
  long registerDepCallbacksTimeNs;
  long allResolversExecutedTimeNs;
  long flushDependencyIfNeededTimeNs;

  public void add(KrystalNodeExecutorMetrics other) {
    commandQueuedCount += other.commandQueuedCount;
    commandQueueBypassedCount += other.commandQueueBypassedCount;
    nodeInputsBatchCount += other.nodeInputsBatchCount;
    depCallbackBatchCount += other.depCallbackBatchCount;
    executeMainLogicCount += other.executeMainLogicCount;
    computeNodeCommandsTimeNs += other.computeNodeCommandsTimeNs;
    propagateNodeCommandsNs += other.propagateNodeCommandsNs;
    executeMainLogicTimeNs += other.executeMainLogicTimeNs;
    registerDepCallbacksTimeNs += other.registerDepCallbacksTimeNs;
    allResolversExecutedTimeNs += other.allResolversExecutedTimeNs;
    flushDependencyIfNeededTimeNs += other.flushDependencyIfNeededTimeNs;
  }

  @Override
  public String toString() {
    return """
              KrystalNodeExecutorMetrics{
                commandQueuedCount              %,d
                commandQueueBypassedCount       %,d
                nodeInputsBatchCount            %,d
                depCallbackBatchCount           %,d
                executeMainLogicCount           %,d
                computeNodeCommandsTime         %s
                propagateNodeCommands           %s
                executeMainLogicTime            %s
                registerDepCallbacksTime        %s
                allResolversExecutedTime        %s
                flushDependencyIfNeededTime     %s
              }
              """
        .formatted(
            commandQueuedCount,
            commandQueueBypassedCount,
            nodeInputsBatchCount,
            depCallbackBatchCount,
            executeMainLogicCount,
            Duration.ofNanos(computeNodeCommandsTimeNs),
            Duration.ofNanos(propagateNodeCommandsNs),
            Duration.ofNanos(executeMainLogicTimeNs),
            Duration.ofNanos(registerDepCallbacksTimeNs),
            Duration.ofNanos(allResolversExecutedTimeNs),
            Duration.ofNanos(flushDependencyIfNeededTimeNs));
  }
}
