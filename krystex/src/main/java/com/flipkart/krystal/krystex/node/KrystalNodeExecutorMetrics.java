package com.flipkart.krystal.krystex.node;

import java.time.Duration;

@SuppressWarnings("PackageVisibleField")
public final class KrystalNodeExecutorMetrics {

  long totalNodeTimeNs;
  long mainLogicIfPossibleTimeNs;
  long computeInputsForExecuteTimeNs;
  long propagateNodeCommandsNs;
  long executeMainLogicTimeNs;
  long handleResolverCommandTimeNs;
  long executeResolversTimeNs;

  long commandQueuedCount;
  long commandQueueBypassedCount;
  long nodeInputsBatchCount;
  long depCallbackBatchCount;
  long executeMainLogicCount;

  public void add(KrystalNodeExecutorMetrics other) {
    totalNodeTimeNs += other.totalNodeTimeNs;
    computeInputsForExecuteTimeNs += other.computeInputsForExecuteTimeNs;
    propagateNodeCommandsNs += other.propagateNodeCommandsNs;
    executeMainLogicTimeNs += other.executeMainLogicTimeNs;
    mainLogicIfPossibleTimeNs += other.mainLogicIfPossibleTimeNs;
    handleResolverCommandTimeNs += other.handleResolverCommandTimeNs;
    executeResolversTimeNs += other.executeResolversTimeNs;

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
                mainLogicIfPossibleTimeNs       %s
                
                computeInputsForExecuteTimeNs   %s
                executeResolversTime            %s
                handleResolverCommandTime       %s
                propagateNodeCommands           %s
                executeMainLogicTime            %s

                commandQueuedCount              %,d
                commandQueueBypassedCount       %,d
                nodeInputsBatchCount            %,d
                depCallbackBatchCount           %,d
                executeMainLogicCount           %,d
              }
              """
        .formatted(
            toString(Duration.ofNanos(totalNodeTimeNs)),
            toString(Duration.ofNanos(mainLogicIfPossibleTimeNs)),
            toString(Duration.ofNanos(computeInputsForExecuteTimeNs)),
            toString(Duration.ofNanos(executeResolversTimeNs)),
            toString(Duration.ofNanos(handleResolverCommandTimeNs)),
            toString(Duration.ofNanos(propagateNodeCommandsNs)),
            toString(Duration.ofNanos(executeMainLogicTimeNs)),
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
