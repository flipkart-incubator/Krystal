package com.flipkart.krystal.krystex.logicdecorators.observability;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
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
public final class DefaultKryonExecutionReport implements KryonExecutionReport {
  @Getter private final Instant startTime;
  private final boolean verbose;
  private final Clock clock;

  @Getter
  private final Map<KryonExecution, LogicExecInfo> mainLogicExecInfos = new LinkedHashMap<>();

  public DefaultKryonExecutionReport(Clock clock) {
    this(clock, false);
  }

  public DefaultKryonExecutionReport(Clock clock, boolean verbose) {
    this.clock = clock;
    this.startTime = clock.instant();
    this.verbose = verbose;
  }

  @Override
  public void reportMainLogicStart(
      KryonId kryonId, KryonLogicId kryonLogicId, ImmutableList<Facets> inputs) {
    KryonExecution kryonExecution =
        new KryonExecution(
            kryonId, inputs.stream().map(this::extractAndConvertInputs).collect(toImmutableList()));
    if (mainLogicExecInfos.containsKey(kryonExecution)) {
      log.error("Cannot start the same kryon execution multiple times: {}", kryonExecution);
      return;
    }
    mainLogicExecInfos.put(
        kryonExecution,
        new LogicExecInfo(
            this, kryonId, inputs, startTime.until(clock.instant(), ChronoUnit.MILLIS)));
  }

  @Override
  public void reportMainLogicEnd(
      KryonId kryonId, KryonLogicId kryonLogicId, Results<Object> result) {
    KryonExecution kryonExecution =
        new KryonExecution(
            kryonId,
            result.values().keySet().stream()
                .map(this::extractAndConvertInputs)
                .collect(toImmutableList()));
    LogicExecInfo logicExecInfo = mainLogicExecInfos.get(kryonExecution);
    if (logicExecInfo == null) {
      log.error(
          "'reportMainLogicEnd' called without calling 'reportMainLogicStart' first for: {}",
          kryonExecution);
      return;
    }
    if (logicExecInfo.getResult() != null) {
      log.error("Cannot end the same kryon execution multiple times: {}", kryonExecution);
      return;
    }
    logicExecInfo.endTimeMs = startTime.until(clock.instant(), ChronoUnit.MILLIS);
    logicExecInfo.setResult(convertResult(result));
  }

  private record KryonExecution(
      KryonId kryonId, ImmutableList<ImmutableMap<String, Object>> inputs) {
    @Override
    public String toString() {
      return "%s(%s)".formatted(kryonId.value(), inputs);
    }
  }

  private ImmutableMap<String, Object> extractAndConvertInputs(Facets facets) {
    Map<String, Object> inputMap = new LinkedHashMap<>();
    for (Entry<String, FacetValue<Object>> e : facets.values().entrySet()) {
      FacetValue<Object> value = e.getValue();
      if (!(value instanceof Errable<Object>)) {
        continue;
      }
      Object collect = convertErrable((Errable<Object>) value);
      if (collect != null) {
        inputMap.put(e.getKey(), collect);
      }
    }
    return ImmutableMap.copyOf(inputMap);
  }

  private ImmutableMap<String, Object> extractAndConvertDependencyResults(Facets facets) {
    Map<String, Object> inputMap = new LinkedHashMap<>();
    for (Entry<String, FacetValue<Object>> e : facets.values().entrySet()) {
      FacetValue<Object> value = e.getValue();
      if (!(value instanceof Results<Object>)) {
        continue;
      }
      Map<ImmutableMap<String, Object>, Object> collect = convertResult((Results<Object>) value);
      inputMap.put(e.getKey(), collect);
    }
    return ImmutableMap.copyOf(inputMap);
  }

  private Object convertErrable(Errable<Object> voe) {
    if (voe.error().isPresent()) {
      Throwable throwable = voe.error().get();
      return verbose ? getStackTraceAsString(throwable) : throwable.toString();
    } else {
      return voe.value().orElse("null");
    }
  }

  private Map<ImmutableMap<String, Object>, Object> convertResult(Results<Object> results) {
    return results.values().entrySet().stream()
        .collect(
            Collectors.toMap(
                e -> extractAndConvertInputs(e.getKey()), e -> convertErrable(e.getValue())));
  }

  @ToString
  @Getter
  static final class LogicExecInfo {

    private final String kryonId;
    private final ImmutableList<ImmutableMap<String, Object>> inputsList;
    private final @Nullable ImmutableList<ImmutableMap<String, Object>> dependencyResults;
    @Nullable private Object result;
    @Getter private final long startTimeMs;
    @Getter private long endTimeMs;

    LogicExecInfo(
        DefaultKryonExecutionReport kryonExecutionReport,
        KryonId kryonId,
        ImmutableCollection<Facets> inputList,
        long startTimeMs) {
      this.startTimeMs = startTimeMs;
      ImmutableList<ImmutableMap<String, Object>> dependencyResults;
      this.kryonId = kryonId.value();
      this.inputsList =
          inputList.stream()
              .map(kryonExecutionReport::extractAndConvertInputs)
              .collect(toImmutableList());
      dependencyResults =
          inputList.stream()
              .map(kryonExecutionReport::extractAndConvertDependencyResults)
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
