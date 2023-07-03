package com.flipkart.krystal.krystex.node;

import java.time.Duration;

public class NodeMetrics {

  private final NodeMetrics delegate;

  private long totalNodeTimeNs;
  private long computeInputsForExecuteTimeNs;
  private long mainLogicIfPossibleTimeNs;
  private long propagateNodeCommandsNs;
  private long executeMainLogicTimeNs;
  private long handleResolverCommandTimeNs;
  private long executeResolversTimeNs;
  private long nodeInputsBatchCount;
  private long depCallbackBatchCount;
  private long executeMainLogicCount;

  public NodeMetrics() {
    this(null);
  }

  public NodeMetrics(NodeMetrics delegate) {
    this.delegate = delegate;
  }

  public void add(NodeMetrics other) {
    totalNodeTimeNs += other.totalNodeTimeNs;
    computeInputsForExecuteTimeNs += other.computeInputsForExecuteTimeNs;
    propagateNodeCommandsNs += other.propagateNodeCommandsNs;
    executeMainLogicTimeNs += other.executeMainLogicTimeNs;
    mainLogicIfPossibleTimeNs += other.mainLogicIfPossibleTimeNs;
    handleResolverCommandTimeNs += other.handleResolverCommandTimeNs;
    executeResolversTimeNs += other.executeResolversTimeNs;

    nodeInputsBatchCount += other.nodeInputsBatchCount;
    depCallbackBatchCount += other.depCallbackBatchCount;
    executeMainLogicCount += other.executeMainLogicCount;
  }

  @Override
  public String toString() {
    return """
              NodeMetrics {
                totalNodeTimeNs                   %s
                  computeInputsForExecuteTimeNs     %s
                  executeResolversTime              %s
                  handleResolverCommandTime         %s
                  propagateNodeCommands             %s
                  mainLogicIfPossibleTimeNs         %s
                    executeMainLogicTime              %s

                nodeInputsBatchCount              %,d
                depCallbackBatchCount             %,d
                executeMainLogicCount             %,d   }
              """
        .formatted(
            toString(Duration.ofNanos(totalNodeTimeNs)),
            toString(Duration.ofNanos(computeInputsForExecuteTimeNs)),
            toString(Duration.ofNanos(executeResolversTimeNs)),
            toString(Duration.ofNanos(handleResolverCommandTimeNs)),
            toString(Duration.ofNanos(propagateNodeCommandsNs)),
            toString(Duration.ofNanos(mainLogicIfPossibleTimeNs)),
            toString(Duration.ofNanos(executeMainLogicTimeNs)),
            nodeInputsBatchCount,
            depCallbackBatchCount,
            executeMainLogicCount);
  }

  private static String toString(Duration duration) {
    return "%dm %ds %dns"
        .formatted(duration.toMinutes(), duration.toSecondsPart(), duration.toNanosPart());
  }

  public void totalNodeTimeNs(long totalNodeTimeNs) {
    this.totalNodeTimeNs += totalNodeTimeNs;
    if (delegate != null) delegate.totalNodeTimeNs(totalNodeTimeNs);
  }

  public void mainLogicIfPossibleTimeNs(long mainLogicIfPossibleTimeNs) {
    this.mainLogicIfPossibleTimeNs += mainLogicIfPossibleTimeNs;
    if (delegate != null) delegate.mainLogicIfPossibleTimeNs(mainLogicIfPossibleTimeNs);
  }

  public void computeInputsForExecuteTimeNs(long computeInputsForExecuteTimeNs) {
    this.computeInputsForExecuteTimeNs += computeInputsForExecuteTimeNs;
    if (delegate != null) delegate.computeInputsForExecuteTimeNs(computeInputsForExecuteTimeNs);
  }

  public void propagateNodeCommandsNs(long propagateNodeCommandsNs) {
    this.propagateNodeCommandsNs += propagateNodeCommandsNs;
    if (delegate != null) delegate.propagateNodeCommandsNs(propagateNodeCommandsNs);
  }

  public void executeMainLogicTimeNs(long executeMainLogicTimeNs) {
    this.executeMainLogicTimeNs += executeMainLogicTimeNs;
    if (delegate != null) delegate.executeMainLogicTimeNs(executeMainLogicTimeNs);
  }

  public void handleResolverCommandTimeNs(long handleResolverCommandTimeNs) {
    this.handleResolverCommandTimeNs += handleResolverCommandTimeNs;
    if (delegate != null) delegate.handleResolverCommandTimeNs(handleResolverCommandTimeNs);
  }

  public void executeResolversTimeNs(long executeResolversTimeNs) {
    this.executeResolversTimeNs += executeResolversTimeNs;
    if (delegate != null) delegate.executeResolversTimeNs(executeResolversTimeNs);
  }

  public void nodeInputsBatchCount() {
    this.nodeInputsBatchCount++;
    if (delegate != null) delegate.nodeInputsBatchCount();
  }

  public void depCallbackBatchCount() {
    this.depCallbackBatchCount++;
    if (delegate != null) delegate.depCallbackBatchCount();
  }

  public void executeMainLogicCount() {
    this.executeMainLogicCount++;
    if (delegate != null) delegate.executeMainLogicCount();
  }
}
