package com.flipkart.krystal.krystex.node;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.time.Instant.now;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.data.ValueOrError;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

@ToString
public final class DefaultNodeExecutionReport implements NodeExecutionReport {
  @Getter private final Instant startTime;

  @Getter
  private final Map<NodeExecution, LogicExecInfo> mainLogicExecInfos = new LinkedHashMap<>();

  public DefaultNodeExecutionReport() {
    this.startTime = now();
  }

  @Override
  public void reportMainLogicStart(
      NodeId nodeId, NodeLogicId nodeLogicId, ImmutableList<Inputs> inputs) {
    LogicExecInfo logicExecInfo =
        mainLogicExecInfos.computeIfAbsent(
            new NodeExecution(
                nodeId,
                inputs.stream()
                    .map(DefaultNodeExecutionReport::extractAndConvertInputs)
                    .collect(toImmutableList())),
            _k -> new LogicExecInfo(nodeId, inputs));
    logicExecInfo.startTimeMs = startTime.until(now(), ChronoUnit.MILLIS);
  }

  @Override
  public void reportMainLogicEnd(NodeId nodeId, NodeLogicId nodeLogicId, Results<Object> result) {
    LogicExecInfo logicExecInfo =
        mainLogicExecInfos.get(
            new NodeExecution(
                nodeId,
                result.values().keySet().stream()
                    .map(DefaultNodeExecutionReport::extractAndConvertInputs)
                    .collect(toImmutableList())));
    logicExecInfo.endTimeMs = startTime.until(now(), ChronoUnit.MILLIS);
    logicExecInfo.setResult(convertResult(result));
  }

  private record NodeExecution(NodeId nodeId, ImmutableList<ImmutableMap<String, Object>> inputs) {
    @Override
    public String toString() {
      return "%s(%s)".formatted(nodeId.value(), inputs);
    }
  }

  private static ImmutableMap<String, Object> extractAndConvertInputs(Inputs inputs) {
    Map<String, Object> inputMap = new LinkedHashMap<>();
    for (Entry<String, InputValue<Object>> e : inputs.values().entrySet()) {
      InputValue<Object> value = e.getValue();
      if (!(value instanceof ValueOrError<Object>)) {
        continue;
      }
      Object collect = convertValueOrError((ValueOrError<Object>) value);
      inputMap.put(e.getKey(), collect);
    }
    return ImmutableMap.copyOf(inputMap);
  }

  private static ImmutableMap<String, Object> extractAndConvertDependencyResults(Inputs inputs) {
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

  private static Object convertValueOrError(ValueOrError<Object> voe) {
    if (voe.error().isPresent()) {
      Throwable throwable = voe.error().get();
      return throwable + "          " + Arrays.toString(throwable.getStackTrace());
    } else {
      return voe.value().orElse(null);
    }
  }

  private static Map<ImmutableMap<String, Object>, Object> convertResult(Results<Object> results) {
    return results.values().entrySet().stream()
        .collect(
            Collectors.toMap(
                e -> extractAndConvertInputs(e.getKey()), e -> convertValueOrError(e.getValue())));
  }

  @ToString
  @Getter
  private static final class LogicExecInfo {

    private final String nodeId;
    private final ImmutableList<ImmutableMap<String, Object>> inputsList;
    private final @Nullable ImmutableList<ImmutableMap<String, Object>> dependencyResults;
    private Object result;
    private long startTimeMs;
    private long endTimeMs;

    LogicExecInfo(NodeId nodeId, ImmutableCollection<Inputs> inputList) {
      ImmutableList<ImmutableMap<String, Object>> dependencyResults;
      this.nodeId = nodeId.value();
      this.inputsList =
          inputList.stream()
              .map(DefaultNodeExecutionReport::extractAndConvertInputs)
              .collect(toImmutableList());
      dependencyResults =
          inputList.stream()
              .map(DefaultNodeExecutionReport::extractAndConvertDependencyResults)
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
