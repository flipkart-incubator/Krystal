package com.flipkart.krystal.krystex.decorators.observability;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@ToString
@Slf4j
public final class DefaultNodeExecutionReport implements NodeExecutionReport {
  @Getter private final Instant startTime;
  private final boolean verbose;
  private final Clock clock;

  @Getter
  private final Map<NodeExecution, LogicExecInfo> mainLogicExecInfos = new LinkedHashMap<>();

  public DefaultNodeExecutionReport(Clock clock) {
    this(clock, false);
  }

  public DefaultNodeExecutionReport(Clock clock, boolean verbose) {
    this.clock = clock;
    this.startTime = clock.instant();
    this.verbose = verbose;
  }

  @Override
  public void reportMainLogicStart(
      NodeId nodeId, NodeLogicId nodeLogicId, ImmutableList<Inputs> inputs) {
    NodeExecution nodeExecution =
        new NodeExecution(
            nodeId, inputs.stream().map(this::extractAndConvertInputs).collect(toImmutableList()));
    if (mainLogicExecInfos.containsKey(nodeExecution)) {
      log.error("Cannot start the same node execution multiple times: {}", nodeExecution);
      return;
    }
    mainLogicExecInfos.put(
        nodeExecution,
        new LogicExecInfo(
            this, nodeId, inputs, startTime.until(clock.instant(), ChronoUnit.MILLIS)));
  }

  @Override
  public void reportMainLogicEnd(NodeId nodeId, NodeLogicId nodeLogicId, Results<Object> result) {
    NodeExecution nodeExecution =
        new NodeExecution(
            nodeId,
            result.values().keySet().stream()
                .map(this::extractAndConvertInputs)
                .collect(toImmutableList()));
    LogicExecInfo logicExecInfo = mainLogicExecInfos.get(nodeExecution);
    if (logicExecInfo == null) {
      log.error(
          "'reportMainLogicEnd' called without calling 'reportMainLogicStart' first for: {}",
          nodeExecution);
      return;
    }
    if (logicExecInfo.getResult() != null) {
      log.error("Cannot end the same node execution multiple times: {}", nodeExecution);
      return;
    }
    logicExecInfo.endTimeMs = startTime.until(clock.instant(), ChronoUnit.MILLIS);
    logicExecInfo.setResult(convertResult(result));
  }

  private record NodeExecution(NodeId nodeId, ImmutableList<ImmutableMap<String, Object>> inputs) {
    @Override
    public String toString() {
      return "%s(%s)".formatted(nodeId.value(), inputs);
    }
  }

  private ImmutableMap<String, Object> extractAndConvertInputs(Inputs inputs) {
    Map<String, Object> inputMap = new LinkedHashMap<>();
    for (Entry<String, InputValue<Object>> e : inputs.values().entrySet()) {
      InputValue<Object> value = e.getValue();
      if (!(value instanceof ValueOrError<Object>)) {
        continue;
      }
      Object collect = convertValueOrError((ValueOrError<Object>) value);
      if (collect != null) {
        inputMap.put(e.getKey(), collect);
      }
    }
    return ImmutableMap.copyOf(inputMap);
  }

  private ImmutableMap<String, Object> extractAndConvertDependencyResults(Inputs inputs) {
    Map<String, Object> inputMap = new LinkedHashMap<>();
    for (Entry<String, InputValue<Object>> e : inputs.values().entrySet()) {
      InputValue<Object> value = e.getValue();
      if (!(value instanceof Results<Object>)) {
        continue;
      }
      Map<ImmutableMap<String, Object>, Object> collect = convertResult((Results<Object>) value);
      inputMap.put(e.getKey(), collect);
    }
    return ImmutableMap.copyOf(inputMap);
  }

  private Object convertValueOrError(ValueOrError<Object> voe) {
    if (voe.error().isPresent()) {
      Throwable throwable = voe.error().get();
      return verbose ? getStackTraceAsString(throwable) : throwable.toString();
    } else {
      return voe.value().orElse(null);
    }
  }

  private Map<ImmutableMap<String, Object>, Object> convertResult(Results<Object> results) {
    return results.values().entrySet().stream()
        .collect(
            Collectors.toMap(
                e -> extractAndConvertInputs(e.getKey()), e -> convertValueOrError(e.getValue())));
  }

  @ToString
  @Getter
  static final class LogicExecInfo {

    private final String nodeId;
    private final ImmutableList<ImmutableMap<String, Object>> inputsList;
    private final @Nullable ImmutableList<ImmutableMap<String, Object>> dependencyResults;
    private Object result;
    @Getter private final long startTimeMs;
    @Getter private long endTimeMs;

    LogicExecInfo(
        DefaultNodeExecutionReport nodeExecutionReport,
        NodeId nodeId,
        ImmutableCollection<Inputs> inputList,
        long startTimeMs) {
      this.startTimeMs = startTimeMs;
      ImmutableList<ImmutableMap<String, Object>> dependencyResults;
      this.nodeId = nodeId.value();
      this.inputsList =
          inputList.stream()
              .map(nodeExecutionReport::extractAndConvertInputs)
              .collect(toImmutableList());
      dependencyResults =
          inputList.stream()
              .map(nodeExecutionReport::extractAndConvertDependencyResults)
              .filter(map -> !map.isEmpty())
              .collect(toImmutableList());
      this.dependencyResults = dependencyResults.isEmpty() ? null : dependencyResults;
    }

    public void setResult(Map<ImmutableMap<String, Object>, Object> result) {
      if (inputsList.size() <= 1 && result.size() == 1) {
        this.result = result.values().iterator().next();
      } else {
        this.result = result;
      }
    }
  }
}
