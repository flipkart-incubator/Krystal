package com.flipkart.krystal.krystex.node;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.ToString;

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
                    .map(DefaultNodeExecutionReport::convertInputs)
                    .collect(ImmutableList.toImmutableList())),
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
                    .map(DefaultNodeExecutionReport::convertInputs)
                    .collect(ImmutableList.toImmutableList())));
    logicExecInfo.endTimeMs = startTime.until(now(), ChronoUnit.MILLIS);
    logicExecInfo.result = convertInputValue(result);
  }

  private record NodeExecution(NodeId nodeId, ImmutableList<ImmutableMap<String, Object>> inputs) {
    @Override
    public String toString() {
      return "%s(%s)".formatted(nodeId.value(), inputs);
    }
  }

  @ToString
  @Getter
  private static final class LogicExecInfo {

    private final String nodeId;
    private final ImmutableList<ImmutableMap<String, Object>> inputs;
    private Object result;
    private long startTimeMs;
    private long endTimeMs;

    LogicExecInfo(NodeId nodeId, ImmutableCollection<Inputs> inputList) {
      this.nodeId = nodeId.value();
      this.inputs =
          inputList.stream()
              .map(inputs -> convertInputs(inputs, false))
              .collect(ImmutableList.toImmutableList());
    }
  }

  private static ImmutableMap<String, Object> convertInputs(Inputs inputs) {
    return convertInputs(inputs, true);
  }

  private static ImmutableMap<String, Object> convertInputs(
      Inputs inputs, boolean omitDependencyResults) {
    Map<String, Object> inputMap = new LinkedHashMap<>();
    for (Entry<String, InputValue<Object>> e : inputs.values().entrySet()) {
      InputValue<Object> value = e.getValue();
      if (omitDependencyResults && value instanceof Results<Object>) {
        continue;
      }
      Object collect = convertInputValue(value);
      inputMap.put(e.getKey(), collect);
    }
    return ImmutableMap.copyOf(inputMap);
  }

  private static Object convertInputValue(InputValue<Object> value) {
    Object collect;
    if (value instanceof ValueOrError<Object> voe) {
      collect = voe.error().isPresent() ? voe.error().get() : voe.value().orElse(null);
    } else if (value instanceof Results<Object> r) {
      collect =
          r.values().entrySet().stream()
              .collect(
                  Collectors.toMap(
                      e -> convertInputs(e.getKey()), e -> convertInputValue(e.getValue())));
    } else {
      throw new UnsupportedOperationException("Unsupported InputValueType");
    }
    return collect;
  }
}
